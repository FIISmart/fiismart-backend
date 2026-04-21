package com.fiismart.backend.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateModuleRequest {

    @NotBlank(message = "Module title is required")
    private String title;

    private String description;
    private int order;
}
