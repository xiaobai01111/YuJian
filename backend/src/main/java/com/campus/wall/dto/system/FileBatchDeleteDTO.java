package com.campus.wall.dto.system;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class FileBatchDeleteDTO {

    @NotEmpty(message = "文件ID不能为空")
    private List<Long> ids;
}
