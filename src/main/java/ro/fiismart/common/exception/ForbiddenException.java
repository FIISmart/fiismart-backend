package ro.fiismart.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Signals an authorization failure (HTTP 403): the caller is authenticated
 * but is not allowed to act on this resource — e.g. submitting/abandoning
 * a quiz attempt that belongs to a different student.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    private final String code;

    public ForbiddenException(String message) {
        super(message);
        this.code = "FORBIDDEN";
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
        this.code = "FORBIDDEN";
    }

    public String getCode() {
        return code;
    }
}