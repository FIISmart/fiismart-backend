package ro.fiismart.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String code;

    public ResourceNotFoundException(String message) {
        super(message);
        this.code = "NOT_FOUND";
    }

    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found: " + id);
        this.code = "NOT_FOUND";
    }

    public String getCode() {
        return code;
    }
}