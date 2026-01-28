package com.campus.wall.controller.auth;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.campus.wall.common.R;
import com.campus.wall.dto.auth.*;
import com.campus.wall.service.auth.AuthService;
import com.campus.wall.vo.auth.LoginVO;
import com.campus.wall.vo.auth.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "用户注册、登录、登出等接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Long> register(@RequestBody @Valid RegisterDTO dto) {
        return R.ok(authService.register(dto));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    @SaCheckLogin
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    @SaCheckLogin
    public R<UserInfoVO> getCurrentUserInfo() {
        return R.ok(authService.getCurrentUserInfo());
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    @SaCheckLogin
    public R<Void> updatePassword(@RequestBody @Valid UpdatePasswordDTO dto) {
        authService.updatePassword(dto);
        return R.ok();
    }

    @Operation(summary = "发送注册邮箱验证码")
    @PostMapping("/register-email-code")
    public R<Void> sendRegisterEmailCode(@RequestBody @Valid RegisterEmailCodeDTO dto) {
        authService.sendRegisterEmailCode(dto.getEmail());
        return R.ok();
    }

    @Operation(summary = "发送EDU邮箱验证码")
    @PostMapping("/verify-email")
    @SaCheckLogin
    public R<Void> sendEmailCode(@RequestBody @Valid VerifyEmailDTO dto) {
        authService.sendEmailCode(dto.getEduEmail());
        return R.ok();
    }

    @Operation(summary = "确认邮箱验证码")
    @PostMapping("/confirm-email")
    @SaCheckLogin
    public R<Void> confirmEmailCode(@RequestBody @Valid ConfirmEmailDTO dto) {
        authService.confirmEmailCode(dto.getCode());
        return R.ok();
    }

    @Operation(summary = "提交学生证进行人工审核")
    @PostMapping("/submit-id-card")
    @SaCheckLogin
    public R<Long> submitIdCard(@RequestBody @Valid SubmitIdCardDTO dto) {
        return R.ok(authService.submitIdCard(dto));
    }

    @Operation(summary = "提交学号认证")
    @PostMapping("/submit-student-id")
    @SaCheckLogin
    public R<Long> submitStudentId(@RequestBody @Valid SubmitStudentIdDTO dto) {
        return R.ok(authService.submitStudentId(dto));
    }

    @Operation(summary = "取消认证申请")
    @PostMapping("/verification/cancel")
    @SaCheckLogin
    public R<Void> cancelVerification() {
        authService.cancelVerification();
        return R.ok();
    }

    @Operation(summary = "获取管理员联系方式")
    @GetMapping("/admin-contact")
    public R<com.campus.wall.vo.auth.AdminContactVO> getAdminContact() {
        return R.ok(authService.getAdminContact());
    }
}
