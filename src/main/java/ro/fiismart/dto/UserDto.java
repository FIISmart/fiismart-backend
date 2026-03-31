package ro.fiismart.dto;

/**
 * The User object sent to the frontend.
 *
 * BRIDGING NOTES — their DB model vs what the frontend expects:
 *
 *  DB model          Frontend expects
 *  displayName   →   firstName + lastName
 *  role "student" →  role "STUDENT"
 *  ObjectId _id  →   String id
 *  (no field)    →   emailVerified
 */
public record UserDto(
        String id,
        String email,
        String firstName,
        String lastName,
        String role,            // "STUDENT" | "TEACHER" | "ADMIN"
        boolean emailVerified,
        String createdAt
) {}
