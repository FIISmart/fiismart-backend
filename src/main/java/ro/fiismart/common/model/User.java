package ro.fiismart.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Users")
public class User {

    @Id
    private String id;

    private String displayName;

    @Indexed(unique = true, sparse = true)
    private String email;

    @Indexed(sparse = true)
    private String cognitoSub;
    /** Username-ul efectiv în Cognito User Pool (email pentru nativi, Google_<sub> pentru federați). */
    private String cognitoUsername;

    private String role;
    /** Rolul salvat înainte de promovarea la admin (folosit la retrogradare). */
    private String baseRole;
    /** True pentru utilizatorii federați (Google) nou creați care nu și-au ales rolul încă. */
    private boolean needsRoleSelection;
    private String passwordHash;
    private boolean banned;
    private String bannedBy;
    private Date bannedAt;
    private String banReason;

    @Builder.Default
    private List<String> ownedCourses = new ArrayList<>();
    @Builder.Default
    private List<String> enrolledCourseIds = new ArrayList<>();
    @Builder.Default
    private List<Session> sessions = new ArrayList<>();

    private Date createdAt;
    private Date lastLoginAt;
}
