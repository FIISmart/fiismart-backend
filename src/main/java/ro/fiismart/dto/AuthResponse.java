package ro.fiismart.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        int expiresIn,
        UserDto user,
        String message
) {
    public static AuthResponse of(String access, String refresh, UserDto user) {
        return new AuthResponse(access, refresh, "Bearer", 3600, user, null);
    }

    public static AuthResponse of(String access, String refresh, UserDto user, String message) {
        return new AuthResponse(access, refresh, "Bearer", 3600, user, message);
    }
}
