package ro.fiismart.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void messageConstructor_setsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Custom message");

        assertThat(ex.getMessage()).isEqualTo("Custom message");
    }

    @Test
    void resourceIdConstructor_formatsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Course", "abc123");

        assertThat(ex.getMessage()).isEqualTo("Course not found: abc123");
    }
}
