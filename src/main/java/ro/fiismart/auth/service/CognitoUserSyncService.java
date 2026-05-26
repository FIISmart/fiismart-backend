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
            boolean dirty = false;
            // Actualizăm cognitoUsername dacă lipsea (migrare date vechi).
            if (existing.getCognitoUsername() == null && cognitoUsername != null) {
                existing.setCognitoUsername(cognitoUsername);
                dirty = true;
            }
            // Promovam rolul în baza grupurilor curente din Cognito. Cognito este
            // sursa de adevăr pentru promovare la admin/professor — fără asta,
            // un user creat ca STUDENT și adăugat ulterior în grupul ADMIN
            // rămâne STUDENT în MongoDB la urmatoarele login-uri. NU coborâm
            // rolul (un PROFESSOR scos din grup rămâne PROFESSOR în DB) pentru
            // a evita downgrade accidental al utilizatorilor cu role manual.
            String groupRole = determineRoleOrNull(groups);
            if (groupRole != null && shouldUpgradeRole(existing.getRole(), groupRole)) {
                log.info("[Sync] Promovare rol pentru {}: {} → {} (din grupuri Cognito)",
                        existing.getEmail(), existing.getRole(), groupRole);
                existing.setRole(groupRole);
                existing.setNeedsRoleSelection(false);
                dirty = true;
            }
            return dirty ? userRepository.save(existing) : existing;
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

    /**
     * Returns the role implied by the Cognito groups, or null when none of
     * the role-bearing groups (ADMIN / PROFESSOR / TEACHER) are present.
     * Used to decide if we should UPGRADE an existing user's role — we
     * deliberately don't want a missing group to downgrade them to student.
     */
    private String determineRoleOrNull(List<String> groups) {
        if (groups == null || groups.isEmpty()) return null;
        for (String g : groups) {
            if (g.equalsIgnoreCase("ADMIN")) return "admin";
            if (g.equalsIgnoreCase("TEACHER") || g.equalsIgnoreCase("PROFESSOR")) return "professor";
        }
        return null;
    }

    /** admin > professor > student > null. Only upgrade, never downgrade. */
    private boolean shouldUpgradeRole(String current, String candidate) {
        return rank(candidate) > rank(current);
    }

    private int rank(String role) {
        if (role == null) return 0;
        return switch (role.toLowerCase()) {
            case "admin" -> 3;
            case "professor", "teacher" -> 2;
            case "student" -> 1;
            default -> 0;
        };
    }
}
