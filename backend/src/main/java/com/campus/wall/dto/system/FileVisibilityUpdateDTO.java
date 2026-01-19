package com.campus.wall.dto.system;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文件可见性修改
 */
@Data
public class FileVisibilityUpdateDTO {

    @NotBlank(message = "visibility不能为空")
    private String visibility;
}
