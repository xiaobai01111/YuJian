package com.campus.wall.controller.console;

import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.user.VerificationHandleDTO;
import com.campus.wall.service.user.VerificationService;
import com.campus.wall.vo.user.VerificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "身份审核管理", description = "后台身份审核相关接口")
@RestController
@RequestMapping("/api/v1/console/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @Operation(summary = "获取待审核列表")
    @GetMapping
    public R<PageResult<VerificationVO>> list(
            @RequestParam(defaultValue = "0") Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return R.ok(verificationService.queryVerifications(status, page, size));
    }

    @Operation(summary = "获取审核详情")
    @GetMapping("/{id}")
    public R<VerificationVO> detail(@PathVariable Long id) {
        return R.ok(verificationService.getVerificationDetail(id));
    }

    @Operation(summary = "处理审核")
    @PutMapping("/{id}")
    public R<Void> handle(@PathVariable Long id, @RequestBody @Valid VerificationHandleDTO dto) {
        verificationService.handleVerification(id, dto);
        return R.ok();
    }
}
