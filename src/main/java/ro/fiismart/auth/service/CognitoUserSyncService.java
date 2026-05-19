package ro.fiismart.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.common.repository.UserRepository;

import java.util.*;

/**
 * La fiecare autentificare Cognito, sincronizează (upsert) utilizatorul în MongoDB.
 *
 * Scenarii gestionate:
 *   1. Sub cunoscut → returnat direct (calea rapidă); dacă emailul era _pending_, actualizat cu merge dacă e nevoie
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
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public User syncUser(String email, String sub, String cognitoUsername,
                         String name, List<String> groups, boolean isFederated) {

        // 1. Cauta după sub — cel mai fiabil identificator.
        Optional<User> bySub = userRepository.findByCognitoSub(sub);
        if (bySub.isPresent()) {
            User existing = bySub.get();
            boolean changed = false;

            if (existing.getCognitoUsername() == null && cognitoUsername != null) {
                existing.setCognitoUsername(cognitoUsername);
                changed = true;
            }

            // Dacă email-ul era placeholder (_pending_), încearcă să îl actualizeze cu cel real.
            String emailNormNow = (email != null && !email.isBlank()) ? email.toLowerCase().trim() : null;
            if (emailNormNow != null && existing.getEmail() != null
                    && existing.getEmail().startsWith("_pending_")) {

                // Verifică dacă există deja alt cont cu emailul real → merge.
                Optional<User> conflict = userRepository.findByEmail(emailNormNow);
                if (conflict.isPresent() && !conflict.get().getId().equals(existing.getId())) {
                    mergeAndArchive(existing, conflict.get());
                }

                log.info("[Sync] Actualizez email placeholder → {} pentru sub={}", emailNormNow, sub);
                existing.setEmail(emailNormNow);
                changed = true;
            }

            if (changed) return userRepository.save(existing);
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
                    log.info("[Sync] Account linking Google→nativ pentru: {} (sub vechi={} nou={})",
                            emailNorm, existing.getCognitoSub(), sub);
                    existing.setCognitoSub(sub);
                    existing.setCognitoUsername(cognitoUsername);
                    return userRepository.save(existing);
                }
                return existing;
            }
        }

        // 4 & 5. Utilizator complet nou.
        boolean hasRoleGroup = groups != null && groups.stream()
                .anyMatch(g -> g.equalsIgnoreCase("STUDENT") || g.equalsIgnoreCase("PROFESSOR")
                        || g.equalsIgnoreCase("TEACHER"));
        boolean needsRoleSelection = isFederated && !hasRoleGroup;
        String role = needsRoleSelection ? null : determineRole(groups);
        String displayName = (name != null && !name.isBlank()) ? name
                : (emailNorm != null ? emailNorm : sub);

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

    /**
     * Absoarbe datele din {@code absorbed} în {@code keeper}, actualizează toate
     * referințele din Courses și Enrollments, apoi arhivează {@code absorbed}
     * (email-ul lui este înlocuit cu un placeholder ca să elibereze indexul unic).
     *
     * {@code keeper} = contul Google (are cognitoSub activ, ID-ul lui rămâne primar)
     * {@code absorbed} = contul care are emailul real, dar va fi absorbit
     */
    private void mergeAndArchive(User keeper, User absorbed) {
        log.info("[Merge] Contopesc contul {} (email={}) în {} (sub={})",
                absorbed.getId(), absorbed.getEmail(), keeper.getId(), keeper.getCognitoSub());

        // Rol: dacă keeper nu are, ia din absorbed.
        if (keeper.getRole() == null && absorbed.getRole() != null) {
            keeper.setRole(absorbed.getRole());
        }
        // needsRoleSelection: false dacă oricare dintre conturi are deja rolul ales.
        if (!absorbed.isNeedsRoleSelection()) {
            keeper.setNeedsRoleSelection(false);
        }

        // displayName: păstrăm al lui keeper (din Google) dacă este valid.
        if ((keeper.getDisplayName() == null || keeper.getDisplayName().isBlank())
                && absorbed.getDisplayName() != null && !absorbed.getDisplayName().isBlank()) {
            keeper.setDisplayName(absorbed.getDisplayName());
        }

        // Union ownedCourses
        List<String> owned = new ArrayList<>(
                keeper.getOwnedCourses() != null ? keeper.getOwnedCourses() : Collections.emptyList());
        if (absorbed.getOwnedCourses() != null) {
            for (String c : absorbed.getOwnedCourses()) {
                if (!owned.contains(c)) owned.add(c);
            }
        }
        keeper.setOwnedCourses(owned);

        // Union enrolledCourseIds
        List<String> enrolled = new ArrayList<>(
                keeper.getEnrolledCourseIds() != null ? keeper.getEnrolledCourseIds() : Collections.emptyList());
        if (absorbed.getEnrolledCourseIds() != null) {
            for (String c : absorbed.getEnrolledCourseIds()) {
                if (!enrolled.contains(c)) enrolled.add(c);
            }
        }
        keeper.setEnrolledCourseIds(enrolled);

        // createdAt: păstrăm cel mai vechi.
        if (absorbed.getCreatedAt() != null
                && (keeper.getCreatedAt() == null || absorbed.getCreatedAt().before(keeper.getCreatedAt()))) {
            keeper.setCreatedAt(absorbed.getCreatedAt());
        }

        // Actualizează Course.teacherId: toate cursurile absorbed → keeper
        List<Course> absorbedCourses = courseRepository.findByTeacherId(absorbed.getId());
        for (Course course : absorbedCourses) {
            course.setTeacherId(keeper.getId());
            courseRepository.save(course);
        }

        // Actualizează Enrollment.studentId: toate înscrierile absorbed → keeper
        List<Enrollment> absorbedEnrollments = enrollmentRepository.findByStudentId(absorbed.getId());
        for (Enrollment enrollment : absorbedEnrollments) {
            enrollment.setStudentId(keeper.getId());
            enrollmentRepository.save(enrollment);
        }

        // Arhivează absorbed: schimbă emailul ca să elibereze indexul unic.
        absorbed.setEmail("_archived_" + absorbed.getId());
        userRepository.save(absorbed);

        log.info("[Merge] Finalizat: {} cursuri + {} înscrieri transferate din {} → {}",
                absorbedCourses.size(), absorbedEnrollments.size(),
                absorbed.getId(), keeper.getId());
    }

    private String determineRole(List<String> groups) {
        if (groups == null || groups.isEmpty()) return "student";
        for (String g : groups) {
            if (g.equalsIgnoreCase("TEACHER") || g.equalsIgnoreCase("PROFESSOR")) return "professor";
        }
        return "student";
    }
}
