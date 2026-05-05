package ro.fiismart.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ro.fiismart.auth.service.CognitoUserSyncService;
import ro.fiismart.common.model.User;

import java.util.List;

/**
 * Convertește un JWT Cognito valid în Authentication cu principalul setat
 * la MongoDB user ID (String). Astfel toate controller-ele existente care
 * folosesc @AuthenticationPrincipal String userId continuă să funcționeze.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CognitoJwtAuthenticationConverter
        implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final CognitoUserSyncService cognitoUserSyncService;

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        String sub = jwt.getSubject();

        // În acest User Pool, Cognito username = adresa de email.
        // Access token-urile NU conțin claim-ul "email" (prezent doar în id_token),
        // dar conțin "username" / "cognito:username" care este chiar emailul.
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("username");
        }
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("cognito:username");
        }

        // "name" claim = display name (prenume + nume); absent din access tokens.
        // syncUser folosește emailul ca fallback pentru display name când name e null.
        String displayName = jwt.getClaimAsString("name");

        List<String> groups = jwt.getClaimAsStringList("cognito:groups");

        User user = cognitoUserSyncService.syncUser(
                email, sub, displayName, groups != null ? groups : List.of()
        );

        String dbRole     = user.getRole() != null ? user.getRole().toLowerCase() : "student";
        String grantedRole = (dbRole.equals("professor") || dbRole.equals("teacher"))
                ? "ROLE_PROFESSOR" : "ROLE_STUDENT";

        log.debug("[JWT] Autentificat: email={} mongoId={} rol={}", email, user.getId(), grantedRole);

        return new UsernamePasswordAuthenticationToken(
                user.getId(), null,
                List.of(new SimpleGrantedAuthority(grantedRole))
        );
    }
}
