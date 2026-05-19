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

@Slf4j
@Component
@RequiredArgsConstructor
public class CognitoJwtAuthenticationConverter
        implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final CognitoUserSyncService cognitoUserSyncService;

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        try {
            String sub = jwt.getSubject();

            // cognito:username → nativi = email (conține "@"), federați = "google_xxx"
            String cognitoUsername = jwt.getClaimAsString("cognito:username");
            if (cognitoUsername == null || cognitoUsername.isBlank()) {
                cognitoUsername = jwt.getClaimAsString("username");
            }

            String email = jwt.getClaimAsString("email");
            if ((email == null || email.isBlank()) && cognitoUsername != null && cognitoUsername.contains("@")) {
                email = cognitoUsername;
            }

            boolean isFederated = cognitoUsername != null && !cognitoUsername.contains("@");

            String displayName = jwt.getClaimAsString("name");
            List<String> groups = jwt.getClaimAsStringList("cognito:groups");

            User user = cognitoUserSyncService.syncUser(
                    email, sub, cognitoUsername, displayName,
                    groups != null ? groups : List.of(), isFederated
            );

            String dbRole = user.getRole() != null ? user.getRole().toLowerCase() : "student";
            String grantedRole = (dbRole.equals("professor") || dbRole.equals("teacher"))
                    ? "ROLE_PROFESSOR" : "ROLE_STUDENT";

            log.debug("[JWT] sub={} email={} federat={} rol={} mongoId={}",
                    sub, email, isFederated, grantedRole, user.getId());

            return new UsernamePasswordAuthenticationToken(
                    user.getId(), null,
                    List.of(new SimpleGrantedAuthority(grantedRole))
            );
        } catch (Exception e) {
            log.error("[JWT] convert() a aruncat excepție — authentication refuzat: {}", e.getMessage(), e);
            return null;
        }
    }
}
