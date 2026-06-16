package com.campus.wall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "审核处理请求")
public class VerificationHandleDTO {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果: 1-通过, 2-拒绝")
    private Integer status;

    @Schema(description = "拒绝原因（拒绝时必填）")
    private String rejectReason;

    @Schema(description = "学号（通过时可填写）")
    private String studentId;

    @Schema(description = "认证方式（通过时可指定）")
    private String verifyMethod;
}
