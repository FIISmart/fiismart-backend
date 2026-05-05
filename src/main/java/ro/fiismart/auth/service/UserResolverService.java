package ro.fiismart.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserResolverService {

    private final UserRepository userRepository;

    /**
     * Extrage email-ul din JWT Cognito și returnează ID-ul MongoDB al utilizatorului.
     * Aruncă 404 dacă utilizatorul nu există în baza de date locală.
     */
    public String resolveMongoId(Jwt jwt) {
        return resolveUser(jwt).getId();
    }

    /**
     * Extrage email-ul din JWT Cognito și returnează entitatea User din MongoDB.
     */
    public User resolveUser(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Token-ul nu conține claim-ul 'email'. Folosiți ID Token-ul din Cognito.");
        }
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Utilizatorul cu email-ul '" + email + "' nu există în baza de date locală."));
    }

    /**
     * Verifică că utilizatorul autentificat este proprietarul resursei cu resourceUserId.
     * Aruncă 403 dacă nu se potrivesc.
     */
    public void validateOwnership(String resourceUserId, Jwt jwt) {
        String authenticatedId = resolveMongoId(jwt);
        if (!authenticatedId.equals(resourceUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Acces interzis: nu aveți dreptul să accesați resursa altui utilizator.");
        }
    }
}
