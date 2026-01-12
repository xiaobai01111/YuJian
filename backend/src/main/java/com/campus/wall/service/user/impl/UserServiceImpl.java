package com.campus.wall.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.user.UserCreateDTO;
import com.campus.wall.dto.user.UserEditDTO;
import com.campus.wall.dto.user.UserQueryDTO;
import com.campus.wall.dto.user.UserUpdateDTO;
import com.campus.wall.entity.system.SysUserRole;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.service.user.UserService;
import com.campus.wall.vo.user.UserDetailVO;
import com.campus.wall.vo.user.UserVO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper sysRoleMapper;

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
        // 禁止禁用管理员账号
        User user = userMapper.selectById(userId);
        if (user != null && "admin".equals(user.getUsername())) {
            throw new RuntimeException("管理员账号不允许禁用");
        }
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
    @Transactional
    public void batchAssignRoles(List<Long> userIds, List<Long> roleIds) {
        for (Long userId : userIds) {
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

    @Override
    @Transactional
    public Long createUser(UserCreateDTO dto) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setDeptId(dto.getDeptId());
        user.setUserType(dto.getUserType() != null ? dto.getUserType() : 0);
        user.setSex(dto.getSex() != null ? dto.getSex() : 0);
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        user.setVerifyStatus(0);
        user.setCreditScore(100);

        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public void editUser(Long userId, UserEditDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getDeptId() != null) user.setDeptId(dto.getDeptId());
        if (dto.getUserType() != null) user.setUserType(dto.getUserType());
        if (dto.getSex() != null) user.setSex(dto.getSex());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());

        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUsers(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        // 删除用户角色关联
        for (Long userId : userIds) {
            userRoleMapper.deleteByUserId(userId);
        }
        // 删除用户
        userMapper.deleteBatchIds(userIds);
    }

    @Override
    public void exportUsers(UserQueryDTO query, HttpServletResponse response) {
        // 查询所有用户（不分页）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(User::getUsername, query.getUsername());
        }
        wrapper.orderByDesc(User::getCreatedAt);
        List<User> users = userMapper.selectList(wrapper);

        // 创建Excel
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("username", "用户名");
        writer.addHeaderAlias("nickname", "昵称");
        writer.addHeaderAlias("email", "邮箱");
        writer.addHeaderAlias("phone", "手机号");
        writer.addHeaderAlias("deptName", "部门");
        writer.addHeaderAlias("userType", "用户类型");
        writer.addHeaderAlias("sex", "性别");
        writer.addHeaderAlias("status", "状态");
        writer.addHeaderAlias("createdAt", "创建时间");

        // 转换数据
        List<Map<String, Object>> rows = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("username", user.getUsername());
            row.put("nickname", user.getNickname());
            row.put("email", user.getEmail());
            row.put("phone", user.getPhone());
            row.put("userType", user.getUserType() == 1 ? "管理员" : "普通用户");
            row.put("sex", user.getSex() == 1 ? "男" : (user.getSex() == 2 ? "女" : "未知"));
            row.put("status", user.getStatus() == 0 ? "正常" : "停用");
            row.put("createdAt", user.getCreatedAt());
            rows.add(row);
        }
        writer.write(rows, true);

        // 输出响应
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        try {
            String fileName = URLEncoder.encode("用户列表.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            writer.flush(response.getOutputStream(), true);
            writer.close();
        } catch (IOException e) {
            throw new BusinessException("导出失败");
        }
    }

    @Override
    @Transactional
    public String importUsers(MultipartFile file, boolean updateExisting) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        try {
            ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
            List<Map<String, Object>> rows = reader.readAll();

            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                try {
                    String username = String.valueOf(row.get("用户名"));
                    String nickname = String.valueOf(row.get("昵称"));
                    String password = row.get("密码") != null ? String.valueOf(row.get("密码")) : "123456";

                    if (!StringUtils.hasText(username) || !StringUtils.hasText(nickname)) {
                        failCount++;
                        errorMsg.append("第").append(i + 2).append("行：用户名或昵称为空\n");
                        continue;
                    }

                    // 检查用户名是否存在
                    User existUser = userMapper.selectOne(
                        new LambdaQueryWrapper<User>().eq(User::getUsername, username)
                    );

                    if (existUser != null) {
                        if (updateExisting) {
                            existUser.setNickname(nickname);
                            if (row.get("邮箱") != null) existUser.setEmail(String.valueOf(row.get("邮箱")));
                            if (row.get("手机号") != null) existUser.setPhone(String.valueOf(row.get("手机号")));
                            userMapper.updateById(existUser);
                            successCount++;
                        } else {
                            failCount++;
                            errorMsg.append("第").append(i + 2).append("行：用户名已存在\n");
                        }
                    } else {
                        User user = new User();
                        user.setUsername(username);
                        user.setNickname(nickname);
                        user.setPassword(BCrypt.hashpw(password));
                        if (row.get("邮箱") != null) user.setEmail(String.valueOf(row.get("邮箱")));
                        if (row.get("手机号") != null) user.setPhone(String.valueOf(row.get("手机号")));
                        user.setVerifyStatus(0);
                        user.setStatus(0);
                        user.setCreditScore(100);
                        userMapper.insert(user);
                        successCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    errorMsg.append("第").append(i + 2).append("行：").append(e.getMessage()).append("\n");
                }
            }
        } catch (IOException e) {
            throw new BusinessException("读取文件失败");
        }

        return String.format("导入完成：成功 %d 条，失败 %d 条。%s", 
            successCount, failCount, failCount > 0 ? "\n" + errorMsg : "");
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) {
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.writeHeadRow(CollUtil.newArrayList("用户名", "昵称", "密码", "邮箱", "手机号"));
        // 写入示例数据
        writer.writeRow(CollUtil.newArrayList("zhangsan", "张三", "123456", "zhangsan@example.com", "13800138000"));

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        try {
            String fileName = URLEncoder.encode("用户导入模板.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            writer.flush(response.getOutputStream(), true);
            writer.close();
        } catch (IOException e) {
            throw new BusinessException("下载模板失败");
        }
    }

    private UserVO toUserVO(User user) {
        Objects.requireNonNull(user, "用户不能为空");
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        
        // 获取用户所有角色（包含禁用的），禁用角色显示后缀
        List<SysRole> roles = sysRoleMapper.selectAllRolesByUserId(user.getId());
        if (roles != null && !roles.isEmpty()) {
            vo.setRoles(roles.stream()
                .map(r -> r.getStatus() == 1 ? r.getRoleName() + "(已禁用)" : r.getRoleName())
                .collect(Collectors.toList()));
        }
        
        return vo;
    }
}
