package ro.fiismart.admin.dto;

import org.junit.jupiter.api.Test;
import ro.fiismart.common.model.User;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUserResponseTest {

    @Test
    void from_adminUser_isAdminTrue() {
        User user = User.builder()
                .id("u1")
                .email("admin@test.com")
                .displayName("Admin")
                .role("admin")
                .banned(false)
                .ownedCourses(List.of("c1", "c2"))
                .enrolledCourseIds(List.of("c3"))
                .createdAt(new Date())
                .build();

        AdminUserResponse response = AdminUserResponse.from(user);

        assertThat(response.isAdmin()).isTrue();
        assertThat(response.getRole()).isEqualTo("admin");
        assertThat(response.getOwnedCoursesCount()).isEqualTo(2);
        assertThat(response.getEnrolledCoursesCount()).isEqualTo(1);
    }

    @Test
    void from_studentUser_isAdminFalse() {
        User user = User.builder()
                .id("u2")
                .role("student")
                .build();

        AdminUserResponse response = AdminUserResponse.from(user);

        assertThat(response.isAdmin()).isFalse();
        assertThat(response.getRole()).isEqualTo("student");
    }

    @Test
    void from_nullRole_defaultsToStudent() {
        User user = User.builder()
                .id("u3")
                .role(null)
                .build();

        AdminUserResponse response = AdminUserResponse.from(user);

        assertThat(response.getRole()).isEqualTo("student");
        assertThat(response.isAdmin()).isFalse();
    }

    @Test
    void from_nullCollections_returnsZeroCounts() {
        User user = User.builder()
                .id("u4")
                .role("student")
                .ownedCourses(null)
                .enrolledCourseIds(null)
                .build();

        AdminUserResponse response = AdminUserResponse.from(user);

        assertThat(response.getOwnedCoursesCount()).isEqualTo(0);
        assertThat(response.getEnrolledCoursesCount()).isEqualTo(0);
    }

    @Test
    void from_bannedUser_setsBannedFields() {
        Date bannedAt = new Date();
        User user = User.builder()
                .id("u5")
                .role("student")
                .banned(true)
                .banReason("Spam")
                .bannedAt(bannedAt)
                .build();

        AdminUserResponse response = AdminUserResponse.from(user);

        assertThat(response.isBanned()).isTrue();
        assertThat(response.getBanReason()).isEqualTo("Spam");
        assertThat(response.getBannedAt()).isEqualTo(bannedAt);
    }

    @Test
    void from_teacherRole_isAdminFalse() {
        User user = User.builder()
                .id("u6")
                .role("teacher")
                .build();

        AdminUserResponse response = AdminUserResponse.from(user);

        assertThat(response.isAdmin()).isFalse();
        assertThat(response.getRole()).isEqualTo("teacher");
    }

    @Test
    void from_adminCaseInsensitive_isAdminTrue() {
        User user = User.builder()
                .id("u7")
                .role("ADMIN")
                .build();

        AdminUserResponse response = AdminUserResponse.from(user);

        assertThat(response.isAdmin()).isTrue();
    }
}
