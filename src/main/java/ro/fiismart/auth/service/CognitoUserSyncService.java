package ro.fiismart.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.UserRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * La fiecare autentificare Cognito, sincronizează (upsert) utilizatorul în MongoDB.
 * Suportă 3 scenarii:
 *   1. Utilizator Cognito existent (găsit după cognitoSub) → returnat direct
 *   2. Utilizator legacy existent cu același email → cognitoSub backfilled
 *   3. Utilizator nou → creat din claim-urile JWT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CognitoUserSyncService {

    private final UserRepository userRepository;

    public User syncUser(String email, String sub, String name, List<String> groups) {
        Optional<User> bySub = userRepository.findByCognitoSub(sub);
        if (bySub.isPresent()) return bySub.get();

        Optional<User> byEmail = userRepository.findByEmail(email.toLowerCase());
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            if (existing.getCognitoSub() == null) {
                existing.setCognitoSub(sub);
                return userRepository.save(existing);
            }
            return existing;
        }

        String role = determineRole(groups);
        String displayName = (name != null && !name.isBlank()) ? name : email;
        User newUser = User.builder()
                .email(email.toLowerCase())
                .displayName(displayName)
                .cognitoSub(sub)
                .role(role)
                .createdAt(new Date())
                .banned(false)
                .build();
        User saved = userRepository.save(newUser);
        log.info("[Cognito] Utilizator nou creat din JWT: {} cu rolul {}", email, role);
        return saved;
    }

    private String determineRole(List<String> groups) {
        if (groups == null || groups.isEmpty()) return "student";
        for (String g : groups) {
            if (g.equalsIgnoreCase("TEACHER") || g.equalsIgnoreCase("PROFESSOR")) return "professor";
        }
        return "student";
    }
}
