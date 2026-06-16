package com.campus.wall.dto.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReportBatchHandleDTO {

    @NotEmpty(message = "举报ID不能为空")
    private List<Long> ids;

    @NotBlank(message = "处理结果不能为空")
    private String result;

    private String remark;
}
