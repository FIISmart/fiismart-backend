package ro.fiismart.users.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.fiismart.users.dto.PublicUserProfileResponse;
import ro.fiismart.users.service.UserProfileService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/{userId}/profile")
    @PreAuthorize("isAuthenticated()")
    public PublicUserProfileResponse getPublicProfile(@PathVariable String userId) {
        return userProfileService.getPublicProfile(userId);
    }
}
