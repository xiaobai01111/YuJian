package com.campus.wall.service.auth;

import com.campus.wall.dto.auth.LoginDTO;
import com.campus.wall.dto.auth.RegisterDTO;
import com.campus.wall.vo.auth.LoginVO;
import com.campus.wall.vo.auth.LoginCaptchaVO;
import com.campus.wall.vo.auth.UserInfoVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     */
    Long register(RegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取登录验证码挑战
     */
    LoginCaptchaVO getLoginCaptcha();

    /**
     * 刷新访问令牌
     */
    LoginVO refreshToken(String refreshToken);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 获取当前用户信息
     */
    UserInfoVO getCurrentUserInfo();

    /**
     * 修改密码
     */
    void updatePassword(com.campus.wall.dto.auth.UpdatePasswordDTO dto);

    /**
     * 发送注册邮箱验证码
     */
    void sendRegisterEmailCode(String email);

    /**
     * 发送EDU邮箱验证码
     */
    void sendEmailCode(String eduEmail);

    /**
     * 确认邮箱验证码
     */
    void confirmEmailCode(String code);

    /**
     * 提交学生证进行人工审核
     */
    Long submitIdCard(com.campus.wall.dto.auth.SubmitIdCardDTO dto);

    /**
     * 提交学号认证申请
     */
    Long submitStudentId(com.campus.wall.dto.auth.SubmitStudentIdDTO dto);

    /**
     * 取消当前待审核认证
     */
    void cancelVerification();

    /**
     * 获取管理员联系方式（公开接口）
     */
    com.campus.wall.vo.auth.AdminContactVO getAdminContact();
}
