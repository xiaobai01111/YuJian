package com.campus.wall.dto.system;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadPolicyUpdateDTO {
    @NotBlank(message = "assetType不能为空")
    private String assetType;
    private String visibility;
}
