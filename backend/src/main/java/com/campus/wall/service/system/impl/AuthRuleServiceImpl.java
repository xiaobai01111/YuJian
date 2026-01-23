package com.campus.wall.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.AuthRuleDTO;
import com.campus.wall.entity.system.SysAuthRule;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.system.SysUserRole;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysAuthRuleMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.service.system.AuthRuleService;
import com.campus.wall.vo.system.AuthRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthRuleServiceImpl implements AuthRuleService {

    private final SysAuthRuleMapper authRuleMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public PageResult<AuthRuleVO> queryRules(int page, int size, String triggerType, String verifyMethod, Boolean enabled) {
        LambdaQueryWrapper<SysAuthRule> wrapper = new LambdaQueryWrapper<>();
        if (triggerType != null && !triggerType.isBlank()) {
            wrapper.eq(SysAuthRule::getTriggerType, triggerType);
        }
        if (verifyMethod != null && !verifyMethod.isBlank()) {
            wrapper.eq(SysAuthRule::getVerifyMethod, verifyMethod);
        }
        if (enabled != null) {
            wrapper.eq(SysAuthRule::getEnabled, enabled);
        }
        wrapper.orderByAsc(SysAuthRule::getPriority).orderByAsc(SysAuthRule::getId);

        Page<SysAuthRule> result = authRuleMapper.selectPage(new Page<>(page, size), wrapper);
        List<AuthRuleVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public Long createRule(AuthRuleDTO dto) {
        SysAuthRule rule = new SysAuthRule();
        copy(dto, rule);
        authRuleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    public void updateRule(Long id, AuthRuleDTO dto) {
        SysAuthRule rule = authRuleMapper.selectById(id);
        if (rule == null) {
            throw new RuntimeException("规则不存在");
        }
        copy(dto, rule);
        rule.setId(id);
        authRuleMapper.updateById(rule);
    }

    @Override
    public void deleteRule(Long id) {
        authRuleMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void applyRules(User user, String triggerType, String verifyMethod) {
        if (user == null || user.getId() == null) {
            return;
        }

        LambdaQueryWrapper<SysAuthRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAuthRule::getEnabled, true)
                .eq(SysAuthRule::getTriggerType, triggerType);
        if (verifyMethod != null) {
            wrapper.and(w -> w.isNull(SysAuthRule::getVerifyMethod)
                .or()
                .eq(SysAuthRule::getVerifyMethod, verifyMethod));
        }
        wrapper.orderByAsc(SysAuthRule::getPriority).orderByAsc(SysAuthRule::getId);

        List<SysAuthRule> rules = authRuleMapper.selectList(wrapper);
        if (rules.isEmpty()) {
            return;
        }

        Set<Long> existingRoleIds = roleMapper.selectAllRolesByUserId(user.getId()).stream()
                .map(SysRole::getId)
                .collect(Collectors.toSet());

        Set<Long> toAddRoles = new HashSet<>();

        for (SysAuthRule rule : rules) {
            if (!match(rule, user)) {
                continue;
            }
            if (rule.getRoleIds() != null) {
                for (Long roleId : rule.getRoleIds()) {
                    if (roleId != null && !existingRoleIds.contains(roleId)) {
                        toAddRoles.add(roleId);
                    }
                }
            }
        }

        for (Long roleId : toAddRoles) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }

    private boolean match(SysAuthRule rule, User user) {
        String matchType = Optional.ofNullable(rule.getMatchType()).orElse("ANY");
        String matchValue = rule.getMatchValue();
        if ("ANY".equalsIgnoreCase(matchType)) {
            return true;
        }
        if (matchValue == null || matchValue.isBlank()) {
            return false;
        }

        if ("EMAIL_DOMAIN".equalsIgnoreCase(matchType)) {
            String email = user.getEduEmail() != null ? user.getEduEmail() : user.getEmail();
            String domain = extractDomain(email);
            if (domain == null) return false;
            return matchAny(domain, matchValue);
        }
        if ("STUDENT_ID_PREFIX".equalsIgnoreCase(matchType)) {
            String studentId = user.getStudentId();
            if (studentId == null) return false;
            return matchAnyPrefix(studentId, matchValue);
        }

        return false;
    }

    private boolean matchAny(String value, String candidates) {
        String[] parts = candidates.split(",");
        for (String part : parts) {
            if (value.equalsIgnoreCase(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchAnyPrefix(String value, String prefixes) {
        String[] parts = prefixes.split(",");
        for (String part : parts) {
            String prefix = part.trim();
            if (!prefix.isEmpty() && value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String extractDomain(String email) {
        if (email == null) return null;
        int idx = email.indexOf('@');
        if (idx < 0 || idx == email.length() - 1) return null;
        return email.substring(idx + 1).toLowerCase();
    }

    private void copy(AuthRuleDTO dto, SysAuthRule rule) {
        rule.setName(dto.getName());
        rule.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        rule.setTriggerType(dto.getTriggerType());
        rule.setVerifyMethod(dto.getVerifyMethod());
        rule.setMatchType(dto.getMatchType());
        rule.setMatchValue(dto.getMatchValue());
        rule.setRoleIds(dto.getRoleIds());
        rule.setPriority(dto.getPriority() != null ? dto.getPriority() : 100);
        rule.setRemark(dto.getRemark());
    }

    private AuthRuleVO toVO(SysAuthRule rule) {
        AuthRuleVO vo = new AuthRuleVO();
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        vo.setEnabled(rule.getEnabled());
        vo.setTriggerType(rule.getTriggerType());
        vo.setVerifyMethod(rule.getVerifyMethod());
        vo.setMatchType(rule.getMatchType());
        vo.setMatchValue(rule.getMatchValue());
        vo.setRoleIds(rule.getRoleIds());
        vo.setPriority(rule.getPriority());
        vo.setRemark(rule.getRemark());
        vo.setCreatedAt(rule.getCreatedAt());
        vo.setUpdatedAt(rule.getUpdatedAt());

        if (rule.getRoleIds() != null && !rule.getRoleIds().isEmpty()) {
            List<SysRole> roles = roleMapper.selectBatchIds(rule.getRoleIds());
            vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
        } else {
            vo.setRoleNames(Collections.emptyList());
        }

        return vo;
    }
}
