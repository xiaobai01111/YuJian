package com.campus.wall.service.user.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.user.VerificationHandleDTO;
import com.campus.wall.entity.user.IdentityVerification;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.file.FileAccessService;
import com.campus.wall.service.system.AuthRuleService;
import com.campus.wall.service.user.VerificationService;
import com.campus.wall.vo.user.VerificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final IdentityVerificationMapper verificationMapper;
    private final UserMapper userMapper;
    private final AuthRuleService authRuleService;
    private final FileAccessService fileAccessService;

    @Override
    public PageResult<VerificationVO> queryVerifications(Integer status, int page, int size) {
        LambdaQueryWrapper<IdentityVerification> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(IdentityVerification::getStatus, status);
        }
        wrapper.orderByDesc(IdentityVerification::getCreatedAt);

        Page<IdentityVerification> result = verificationMapper.selectPage(
            new Page<>(page, size), wrapper
        );

        List<VerificationVO> records = result.getRecords().stream()
            .map(this::toVO)
            .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public VerificationVO getVerificationDetail(Long id) {
        IdentityVerification verification = verificationMapper.selectById(id);
        if (verification == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toVO(verification);
    }

    @Override
    @Transactional
    public void handleVerification(Long id, VerificationHandleDTO dto) {
        IdentityVerification verification = verificationMapper.selectById(id);
        if (verification == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (verification.getStatus() != 0) {
            throw new BusinessException("该申请已处理");
        }

        Long reviewerId = StpUtil.getLoginIdAsLong();

        verification.setStatus(dto.getStatus());
        verification.setReviewerId(reviewerId);
        verification.setReviewedAt(LocalDateTime.now());

        if (dto.getStatus() == 2) {
            // 拒绝
            if (dto.getRejectReason() == null || dto.getRejectReason().isEmpty()) {
                throw new BusinessException("拒绝时必须填写原因");
            }
            verification.setRejectReason(dto.getRejectReason());
            User user = userMapper.selectById(verification.getUserId());
            if (user != null) {
                user.setVerifyStatus(0);
                user.setVerifyMethod(null);
                userMapper.updateById(user);
            }
        } else if (dto.getStatus() == 1) {
            // 通过 - 更新用户验证状态
            User user = userMapper.selectById(verification.getUserId());
            user.setVerifyStatus(2); // 已验证
            String verifyMethod = dto.getVerifyMethod();
            if (!StringUtils.hasText(verifyMethod)) {
                verifyMethod = StringUtils.hasText(verification.getVerifyMethod())
                    ? verification.getVerifyMethod()
                    : "MANUAL";
            }
            user.setVerifyMethod(verifyMethod);
            verification.setVerifyMethod(verifyMethod);

            // 如果提供了学号
            if (dto.getStudentId() != null && !dto.getStudentId().isEmpty()) {
                String studentIdHash = SecureUtil.sha256(dto.getStudentId());
                
                // 检查学号唯一性
                Long existCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getStudentIdHash, studentIdHash)
                        .ne(User::getId, user.getId())
                );
                if (existCount > 0) {
                    throw new BusinessException(ResultCode.STUDENT_ID_EXISTS);
                }

                user.setStudentId(dto.getStudentId());
                user.setStudentIdHash(studentIdHash);
                verification.setStudentId(dto.getStudentId());
                verification.setStudentIdHash(studentIdHash);
            }

            userMapper.updateById(user);

            // 认证通过后应用规则
            authRuleService.applyRules(user, "VERIFY", verifyMethod);
        }

        verificationMapper.updateById(verification);
    }

    private VerificationVO toVO(IdentityVerification verification) {
        VerificationVO vo = new VerificationVO();
        vo.setId(verification.getId());
        vo.setUserId(verification.getUserId());
        String imageUrl = verification.getImageUrl();
        if (StringUtils.hasText(imageUrl) && imageUrl.chars().allMatch(Character::isDigit)) {
            try {
                imageUrl = fileAccessService.buildSignedPreviewUrl(Long.valueOf(imageUrl));
            } catch (NumberFormatException ignored) {
            }
        }
        vo.setImageUrl(imageUrl);
        vo.setVerifyMethod(verification.getVerifyMethod());
        vo.setStudentId(verification.getStudentId());
        vo.setStatus(verification.getStatus());
        vo.setRejectReason(verification.getRejectReason());
        vo.setReviewerId(verification.getReviewerId());
        vo.setCreatedAt(verification.getCreatedAt());
        vo.setReviewedAt(verification.getReviewedAt());

        // 获取用户信息
        User user = userMapper.selectById(verification.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
        }

        // 获取审核员信息
        if (verification.getReviewerId() != null) {
            User reviewer = userMapper.selectById(verification.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerName(reviewer.getNickname());
            }
        }

        return vo;
    }
}
