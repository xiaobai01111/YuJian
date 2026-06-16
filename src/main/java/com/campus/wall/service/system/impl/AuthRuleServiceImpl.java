package com.campus.wall.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.AuthRuleDTO;
import com.campus.wall.entity.system.SysAuthRule;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.system.SysUserRole;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysAuthRuleMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.service.system.AuthRuleService;
import com.campus.wall.service.system.SysConfigService;
import com.campus.wall.vo.system.AuthRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthRuleServiceImpl implements AuthRuleService {

    private final SysAuthRuleMapper authRuleMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysConfigService sysConfigService;

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
    public AuthRuleVO getRuleById(Long id) {
        SysAuthRule rule = authRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        return toVO(rule);
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
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
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
    public void updateStatus(Long id, Boolean enabled) {
        SysAuthRule rule = authRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        rule.setEnabled(enabled != null ? enabled : Boolean.TRUE);
        authRuleMapper.updateById(rule);
    }

    @Override
    public void updatePriority(Long id, Integer priority) {
        if (priority == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优先级不能为空");
        }
        SysAuthRule rule = authRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        rule.setPriority(priority);
        authRuleMapper.updateById(rule);
    }

    @Override
    public Long cloneRule(Long id) {
        SysAuthRule rule = authRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        SysAuthRule copy = new SysAuthRule();
        copy.setName(buildCloneName(rule.getName()));
        copy.setEnabled(rule.getEnabled());
        copy.setTriggerType(rule.getTriggerType());
        copy.setVerifyMethod(rule.getVerifyMethod());
        copy.setMatchType(rule.getMatchType());
        copy.setMatchValue(rule.getMatchValue());
        copy.setRoleIds(rule.getRoleIds() != null ? new ArrayList<>(rule.getRoleIds()) : null);
        copy.setPriority(rule.getPriority());
        copy.setRemark(rule.getRemark());
        authRuleMapper.insert(copy);
        return copy.getId();
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

        Set<Long> toAssignRoles = new HashSet<>();

        for (SysAuthRule rule : rules) {
            if (!match(rule, user)) {
                continue;
            }
            if (rule.getRoleIds() != null) {
                for (Long roleId : rule.getRoleIds()) {
                    if (roleId != null) {
                        toAssignRoles.add(roleId);
                    }
                }
            }
        }

        if (toAssignRoles.isEmpty()) {
            return;
        }

        // 强制单角色：先删除用户所有现有角色，再分配新角色
        userRoleMapper.delete(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId())
        );

        for (Long roleId : toAssignRoles) {
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
            String candidates = matchValue;
            if (!StringUtils.hasText(candidates)) {
                candidates = String.join(",", sysConfigService.getEmailAllowedDomains());
            }
            return matchAny(domain, candidates);
        }
        if ("STUDENT_ID_PREFIX".equalsIgnoreCase(matchType)) {
            String studentId = user.getStudentId();
            if (studentId == null) return false;
            return matchAnyPrefix(studentId, matchValue);
        }
        if ("STUDENT_ID_RANGE".equalsIgnoreCase(matchType)) {
            String studentId = user.getStudentId();
            if (studentId == null) return false;
            return matchRange(studentId, matchValue);
        }
        if ("STUDENT_ID_DICT".equalsIgnoreCase(matchType)) {
            String studentId = user.getStudentId();
            if (studentId == null) return false;
            List<String> list = sysConfigService.getStudentIdWhitelist();
            return list.stream().anyMatch(studentId::equals);
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

    private boolean matchRange(String value, String range) {
        if (!StringUtils.hasText(range)) {
            return false;
        }
        String[] parts = range.split("-");
        if (parts.length != 2) {
            return false;
        }
        String start = parts[0].trim();
        String end = parts[1].trim();
        if (start.isEmpty() || end.isEmpty()) {
            return false;
        }

        if (value.matches("\\d+") && start.matches("\\d+") && end.matches("\\d+")) {
            try {
                long v = Long.parseLong(value);
                long s = Long.parseLong(start);
                long e = Long.parseLong(end);
                return v >= s && v <= e;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        return value.compareTo(start) >= 0 && value.compareTo(end) <= 0;
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

    private String buildCloneName(String name) {
        String base = StringUtils.hasText(name) ? name.trim() : "规则";
        return base + " - 副本";
    }
}
