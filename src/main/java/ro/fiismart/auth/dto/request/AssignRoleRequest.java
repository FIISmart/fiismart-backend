package ro.fiismart.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AssignRoleRequest {

    @NotBlank(message = "Rolul este obligatoriu")
    @Pattern(regexp = "(?i)STUDENT|PROFESSOR", message = "Rolul trebuie să fie STUDENT sau PROFESSOR")
    private String role;

    private String firstName;
    private String lastName;
}
