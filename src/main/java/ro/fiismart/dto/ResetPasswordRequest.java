package ro.fiismart.dto;

public record ResetPasswordRequest(String token, String newPassword, String confirmPassword) {}
