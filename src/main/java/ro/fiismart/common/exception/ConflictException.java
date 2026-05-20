package ro.fiismart.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Signals a state conflict (HTTP 409). Typical uses: attempting to submit
 * a quiz attempt that has already been submitted/abandoned, or any other
 * operation that the resource's current state forbids.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
