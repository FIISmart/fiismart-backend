package ro.fiismart.dto;

public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        String role          // "STUDENT" or "TEACHER" — frontend sends uppercase
) {}
