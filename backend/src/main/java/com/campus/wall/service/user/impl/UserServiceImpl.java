package com.campus.wall.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.user.UserQueryDTO;
import com.campus.wall.dto.user.UserUpdateDTO;
import com.campus.wall.entity.system.SysUserRole;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.user.UserService;
import com.campus.wall.vo.user.UserDetailVO;
import com.campus.wall.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper sysDeptMapper;

    @Override
    public PageResult<UserVO> queryUsers(UserQueryDTO query) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(User::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getNickname())) {
            wrapper.like(User::getNickname, query.getNickname());
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        if (query.getVerifyStatus() != null) {
            wrapper.eq(User::getVerifyStatus, query.getVerifyStatus());
        }
        
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> page = userMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()), wrapper
        );

        List<UserVO> records = page.getRecords().stream()
            .map(this::toUserVO)
            .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    public UserDetailVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        UserDetailVO vo = new UserDetailVO();
        BeanUtils.copyProperties(user, vo);

        // 获取用户角色ID列表
        List<SysUserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );
        vo.setRoleIds(userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public void updateUser(Long userId, UserUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }

        userMapper.updateById(user);
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        userMapper.updateStatus(userId, status);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 删除原有角色
        userRoleMapper.deleteByUserId(userId);

        // 添加新角色
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }

    @Override
    public void updateCreditScore(Long userId, int delta, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        int newScore = Math.max(0, Math.min(100, user.getCreditScore() + delta));
        userMapper.updateCreditScore(userId, newScore);
    }

    private UserVO toUserVO(User user) {
        Objects.requireNonNull(user, "用户不能为空");
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        
        // 获取部门名称
        if (user.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(user.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }
        return vo;
    }
}
