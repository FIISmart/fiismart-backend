package ro.fiismart.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.UserRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * La fiecare autentificare Cognito, sincronizează (upsert) utilizatorul în MongoDB.
 *
 * Scenarii gestionate:
 *   1. Sub cunoscut → returnat direct (calea rapidă)
 *   2. Email existent, nativ fără sub → backfill sub
 *   3. Email existent, federare Google → account linking (actualizăm sub + cognitoUsername)
 *   4. Utilizator complet nou, nativ → creat cu rol din grup Cognito
 *   5. Utilizator complet nou, Google → creat cu needsRoleSelection = true
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CognitoUserSyncService {

    private final UserRepository userRepository;

    public User syncUser(String email, String sub, String cognitoUsername,
                         String name, List<String> groups, boolean isFederated) {

        // 1. Cauta după sub — cel mai fiabil identificator.
        Optional<User> bySub = userRepository.findByCognitoSub(sub);
        if (bySub.isPresent()) {
            User existing = bySub.get();
            // Actualizăm cognitoUsername dacă lipsea (migrare date vechi).
            if (existing.getCognitoUsername() == null && cognitoUsername != null) {
                existing.setCognitoUsername(cognitoUsername);
                return userRepository.save(existing);
            }
            return existing;
        }

        String emailNorm = (email != null && !email.isBlank()) ? email.toLowerCase().trim() : null;

        if (emailNorm != null) {
            Optional<User> byEmail = userRepository.findByEmail(emailNorm);
            if (byEmail.isPresent()) {
                User existing = byEmail.get();
                if (existing.getCognitoSub() == null) {
                    // 2. Backfill sub pentru cont legacy fără sub.
                    existing.setCognitoSub(sub);
                    existing.setCognitoUsername(cognitoUsername);
                    log.info("[Sync] Backfill cognitoSub pentru: {}", emailNorm);
                    return userRepository.save(existing);
                }
                if (isFederated) {
                    // 3. Account linking: utilizatorul are deja cont nativ, acum se loghează cu Google.
                    // Actualizăm sub-ul și username-ul Cognito → viitoarele login-uri Google găsesc direct.
                    log.info("[Sync] Account linking Google→nativ pentru: {} (sub vechi={} nou={})",
                            emailNorm, existing.getCognitoSub(), sub);
                    existing.setCognitoSub(sub);
                    existing.setCognitoUsername(cognitoUsername);
                    return userRepository.save(existing);
                }
                // Nativi cu sub diferit (nu ar trebui să se întâmple în practică): returnam cont existent.
                return existing;
            }
        }

        // 4 & 5. Utilizator complet nou.
        // Grupul intern Cognito (ex: "eu-north-1_puAbduwjE_Google") nu înseamnă rol ales.
        boolean hasRoleGroup = groups != null && groups.stream()
                .anyMatch(g -> g.equalsIgnoreCase("STUDENT") || g.equalsIgnoreCase("PROFESSOR")
                        || g.equalsIgnoreCase("TEACHER") || g.equalsIgnoreCase("ADMIN"));
        boolean needsRoleSelection = isFederated && !hasRoleGroup;
        String role = needsRoleSelection ? null : determineRole(groups);
        String displayName = (name != null && !name.isBlank()) ? name
                : (emailNorm != null ? emailNorm : sub);

        // Dacă email lipsește (token federat fără claim email),
        // generăm placeholder unic bazat pe sub — evită conflicte de index null.
        String emailToStore = (emailNorm != null) ? emailNorm : ("_pending_" + sub);

        User newUser = User.builder()
                .email(emailToStore)
                .displayName(displayName)
                .cognitoSub(sub)
                .cognitoUsername(cognitoUsername)
                .role(role)
                .needsRoleSelection(needsRoleSelection)
                .createdAt(new Date())
                .banned(false)
                .build();

        try {
            User saved = userRepository.save(newUser);
            log.info("[Sync] Utilizator nou creat: email={} federat={} needsRoleSelection={} rol={}",
                    emailToStore, isFederated, needsRoleSelection, role);
            return saved;
        } catch (DuplicateKeyException e) {
            log.warn("[Sync] DuplicateKey sub={} email={} — recuperez document existent", sub, emailToStore);
            return userRepository.findByCognitoSub(sub)
                    .or(() -> userRepository.findByEmail(emailToStore))
                    .orElseThrow(() -> new RuntimeException("Nu s-a putut crea sau recupera utilizatorul: " + sub));
        }
    }

    private String determineRole(List<String> groups) {
        if (groups == null || groups.isEmpty()) return "student";
        for (String g : groups) {
            if (g.equalsIgnoreCase("ADMIN")) return "admin";
            if (g.equalsIgnoreCase("TEACHER") || g.equalsIgnoreCase("PROFESSOR")) return "professor";
        }
        return "student";
    }
}
