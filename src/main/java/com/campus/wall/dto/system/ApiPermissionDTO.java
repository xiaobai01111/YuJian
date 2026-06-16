package com.campus.wall.dto.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApiPermissionDTO {

    @NotBlank
    @Size(max = 255)
    private String url;

    @Size(max = 10)
    private String httpMethod;

    @NotBlank
    @Size(max = 100)
    private String permission;

    @Size(max = 255)
    private String description;

    private Boolean status;
}
