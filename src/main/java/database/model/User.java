package database.model;

import lombok.Builder;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class User {

    private ObjectId id;
    private String displayName;
    private String email;
    private boolean banned;
    private String role; // "teacher" | "student" | "admin"
    private Date createdAt;
    private Date lastLoginAt;
    private String passwordHash;

    // ban fields (teacher/student)
    private ObjectId bannedBy;
    private Date bannedAt;
    private String banReason;

    // teacher only
    @Builder.Default
    private List<ObjectId> ownedCourses = new ArrayList<>();

    // student only
    @Builder.Default
    private List<ObjectId> enrolledCourseIds = new ArrayList<>();
    @Builder.Default
    private List<Document> sessions = new ArrayList<>();

    public Document toDocument() {
        Document doc = new Document()
                .append("_id", id != null ? id : new ObjectId())
                .append("displayName", displayName)
                .append("email", email)
                .append("banned", banned)
                .append("role", role)
                .append("createdAt", createdAt)
                .append("lastLoginAt", lastLoginAt)
                .append("passwordHash", passwordHash)
                .append("bannedBy", bannedBy)
                .append("bannedAt", bannedAt)
                .append("banReason", banReason);

        if ("teacher".equals(role)) {
            doc.append("ownedCourses", ownedCourses != null ? ownedCourses : new ArrayList<>());
        } else if ("student".equals(role)) {
            doc.append("enrolledCourseIds", enrolledCourseIds != null ? enrolledCourseIds : new ArrayList<>());
            doc.append("sessions", sessions != null ? sessions : new ArrayList<>());
        }

        return doc;
    }

    public static User fromDocument(Document doc) {
        if (doc == null) return null;
        return User.builder()
                .id(doc.getObjectId("_id"))
                .displayName(doc.getString("displayName"))
                .email(doc.getString("email"))
                .banned(Boolean.TRUE.equals(doc.getBoolean("banned")))
                .role(doc.getString("role"))
                .createdAt(doc.getDate("createdAt"))
                .lastLoginAt(doc.getDate("lastLoginAt"))
                .passwordHash(doc.getString("passwordHash"))
                .bannedBy(doc.getObjectId("bannedBy"))
                .bannedAt(doc.getDate("bannedAt"))
                .banReason(doc.getString("banReason"))
                .ownedCourses(doc.getList("ownedCourses", ObjectId.class) != null
                        ? doc.getList("ownedCourses", ObjectId.class) : new ArrayList<>())
                .enrolledCourseIds(doc.getList("enrolledCourseIds", ObjectId.class) != null
                        ? doc.getList("enrolledCourseIds", ObjectId.class) : new ArrayList<>())
                .build();
    }
}
