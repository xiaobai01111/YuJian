package com.campus.wall.service.user.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.service.system.AuthRuleService;
import com.campus.wall.service.user.VerificationService;
import com.campus.wall.util.SensitiveFieldUtil;
import com.campus.wall.vo.user.VerificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private static final String VERIFICATION_VIEW_PERMISSION = "content:verification:view";
    private static final String VERIFICATION_HANDLE_PERMISSION = "content:verification:handle";

    private final IdentityVerificationMapper verificationMapper;
    private final UserMapper userMapper;
    private final AuthRuleService authRuleService;
    private final FileAccessService fileAccessService;
    private final DataScopeService dataScopeService;

    @Override
    public PageResult<VerificationVO> queryVerifications(Integer status, int page, int size) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        DataScopeService.DataScope scope = dataScopeService.resolveScope(operatorId);
        LambdaQueryWrapper<IdentityVerification> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(IdentityVerification::getStatus, status);
        }
        applyVerificationDataScope(wrapper, scope, operatorId);
        wrapper.orderByDesc(IdentityVerification::getCreatedAt);

        Page<IdentityVerification> result = verificationMapper.selectPage(
            new Page<>(page, size), wrapper
        );

        List<VerificationVO> records = toVOList(result.getRecords());

        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public VerificationVO getVerificationDetail(Long id) {
        IdentityVerification verification = verificationMapper.selectById(id);
        if (verification == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ensureCanAccessVerification(verification, "无权查看该审核记录");
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
        ensureCanAccessVerification(verification, "无权处理该审核记录");

        Long reviewerId = StpUtil.getLoginIdAsLong();

        verification.setStatus(dto.getStatus());
        verification.setReviewerId(reviewerId);
        verification.setReviewedAt(LocalDateTime.now());

        User user = null;
        if (dto.getStatus() == 2) {
            // 拒绝
            if (dto.getRejectReason() == null || dto.getRejectReason().isEmpty()) {
                throw new BusinessException("拒绝时必须填写原因");
            }
            verification.setRejectReason(dto.getRejectReason());
            user = userMapper.selectById(verification.getUserId());
            if (user != null) {
                user.setVerifyStatus(0);
                user.setVerifyMethod(null);
            }
        } else if (dto.getStatus() == 1) {
            // 通过 - 更新用户验证状态
            user = userMapper.selectById(verification.getUserId());
            if (user == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
            }
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
                String studentId = SensitiveFieldUtil.normalizeStudentId(dto.getStudentId());
                String studentIdHash = SensitiveFieldUtil.hashStudentId(studentId);
                
                // 检查学号唯一性
                Long existCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getStudentIdHash, studentIdHash)
                        .ne(User::getId, user.getId())
                );
                if (existCount > 0) {
                    throw new BusinessException(ResultCode.STUDENT_ID_EXISTS);
                }

                user.setStudentId(studentId);
                user.setStudentIdHash(studentIdHash);
                verification.setStudentId(studentId);
                verification.setStudentIdHash(studentIdHash);
            }

        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "审核结果不合法");
        }

        LambdaUpdateWrapper<IdentityVerification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(IdentityVerification::getId, verification.getId())
            .eq(IdentityVerification::getStatus, 0)
            .set(IdentityVerification::getStatus, verification.getStatus())
            .set(IdentityVerification::getReviewerId, verification.getReviewerId())
            .set(IdentityVerification::getReviewedAt, verification.getReviewedAt());
        if (verification.getRejectReason() != null) {
            updateWrapper.set(IdentityVerification::getRejectReason, verification.getRejectReason());
        }
        if (verification.getVerifyMethod() != null) {
            updateWrapper.set(IdentityVerification::getVerifyMethod, verification.getVerifyMethod());
        }
        if (verification.getStudentId() != null) {
            updateWrapper.set(IdentityVerification::getStudentId, verification.getStudentId());
        }
        if (verification.getStudentIdHash() != null) {
            updateWrapper.set(IdentityVerification::getStudentIdHash, verification.getStudentIdHash());
        }
        int updated = verificationMapper.update(null, updateWrapper);
        if (updated == 0) {
            throw new BusinessException("该申请已处理");
        }

        if (user != null) {
            userMapper.updateById(user);
        }
        if (dto.getStatus() == 1 && user != null) {
            String verifyMethod = verification.getVerifyMethod();
            authRuleService.applyRules(user, "VERIFY", verifyMethod);
        }
    }

    private void ensureCanAccessVerification(IdentityVerification verification, String message) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        Long targetUserId = verification.getUserId();
        if (operatorId == null || targetUserId == null || !dataScopeService.canAccessUser(operatorId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, message);
        }
    }

    private void applyVerificationDataScope(
        LambdaQueryWrapper<IdentityVerification> wrapper,
        DataScopeService.DataScope scope,
        Long operatorId) {
        if (scope == null || scope.isAllowAll()) {
            return;
        }
        String deptScopeSql = dataScopeService.buildUserScopeExistsSql(scope, "identity_verifications.user_id");
        if (deptScopeSql == null) {
            if (scope.isAllowSelf() && operatorId != null) {
                wrapper.eq(IdentityVerification::getUserId, operatorId);
            } else {
                wrapper.eq(IdentityVerification::getId, -1L);
            }
            return;
        }
        if (scope.isAllowSelf() && operatorId != null) {
            wrapper.and(w -> w.eq(IdentityVerification::getUserId, operatorId).or().apply(deptScopeSql));
        } else {
            wrapper.apply(deptScopeSql);
        }
    }

    private VerificationVO toVO(IdentityVerification verification) {
        Map<Long, User> userMap = new HashMap<>();
        if (verification.getUserId() != null) {
            User user = userMapper.selectById(verification.getUserId());
            if (user != null) {
                userMap.put(user.getId(), user);
            }
        }
        if (verification.getReviewerId() != null) {
            User reviewer = userMapper.selectById(verification.getReviewerId());
            if (reviewer != null) {
                userMap.put(reviewer.getId(), reviewer);
            }
        }
        return toVO(verification, userMap);
    }

    private List<VerificationVO> toVOList(List<IdentityVerification> verifications) {
        if (verifications == null || verifications.isEmpty()) {
            return List.of();
        }
        Set<Long> userIdSet = new HashSet<>();
        for (IdentityVerification verification : verifications) {
            if (verification == null) {
                continue;
            }
            if (verification.getUserId() != null) {
                userIdSet.add(verification.getUserId());
            }
            if (verification.getReviewerId() != null) {
                userIdSet.add(verification.getReviewerId());
            }
        }
        Map<Long, User> userMap = userIdSet.isEmpty()
            ? Map.of()
            : userMapper.selectBatchIds(new ArrayList<>(userIdSet)).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
        List<VerificationVO> result = new ArrayList<>(verifications.size());
        for (IdentityVerification verification : verifications) {
            if (verification == null) {
                continue;
            }
            result.add(toVO(verification, userMap));
        }
        return result;
    }

    private VerificationVO toVO(IdentityVerification verification, Map<Long, User> userMap) {
        VerificationVO vo = new VerificationVO();
        vo.setId(verification.getId());
        vo.setUserId(verification.getUserId());
        vo.setImageUrl(resolveEvidenceImageUrl(verification.getImageUrl()));
        vo.setVerifyMethod(verification.getVerifyMethod());
        vo.setStudentId(verification.getStudentId());
        vo.setStatus(verification.getStatus());
        vo.setRejectReason(verification.getRejectReason());
        vo.setReviewerId(verification.getReviewerId());
        vo.setCreatedAt(verification.getCreatedAt());
        vo.setReviewedAt(verification.getReviewedAt());

        // 获取用户信息
        User user = verification.getUserId() == null ? null : userMap.get(verification.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
        }

        // 获取审核员信息
        if (verification.getReviewerId() != null) {
            User reviewer = userMap.get(verification.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerName(reviewer.getNickname());
            }
        }

        return vo;
    }

    private String resolveEvidenceImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        if (!canReadEvidenceMaterial()) {
            return null;
        }
        if (imageUrl.chars().allMatch(Character::isDigit)) {
            try {
                return fileAccessService.buildSignedPreviewUrl(Long.valueOf(imageUrl));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return imageUrl;
    }

    private boolean canReadEvidenceMaterial() {
        if (!StpUtil.isLogin()) {
            return false;
        }
        return StpUtil.hasPermission(VERIFICATION_VIEW_PERMISSION)
            || StpUtil.hasPermission(VERIFICATION_HANDLE_PERMISSION);
    }
}
