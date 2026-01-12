package com.campus.wall.controller.console;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.user.BatchUserRoleDTO;
import com.campus.wall.dto.user.UserBanDTO;
import com.campus.wall.dto.user.UserCreateDTO;
import com.campus.wall.dto.user.UserEditDTO;
import com.campus.wall.dto.user.UserQueryDTO;
import com.campus.wall.dto.user.UserRoleDTO;
import com.campus.wall.service.user.UserService;
import com.campus.wall.vo.user.UserDetailVO;
import com.campus.wall.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户管理接口（后台）
 */
@Tag(name = "用户管理", description = "后台用户管理接口")
@RestController
@RequestMapping("/api/v1/console/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户列表", description = "分页查询用户列表")
    @SaCheckPermission("system:user:list")
    @GetMapping
    public R<PageResult<UserVO>> list(UserQueryDTO query) {
        return R.ok(userService.queryUsers(query));
    }

    @Operation(summary = "用户详情", description = "获取用户详细信息")
    @SaCheckPermission("system:user:list")
    @GetMapping("/{id}")
    public R<UserDetailVO> detail(@PathVariable Long id) {
        return R.ok(userService.getUserDetail(id));
    }

    @Operation(summary = "新增用户", description = "创建新用户")
    @SaCheckPermission("system:user:add")
    @PostMapping
    public R<Long> create(@RequestBody @Valid UserCreateDTO dto) {
        return R.ok(userService.createUser(dto));
    }

    @Operation(summary = "修改用户", description = "修改用户信息")
    @SaCheckPermission("system:user:edit")
    @PutMapping("/{id}")
    public R<Void> edit(@PathVariable Long id, @RequestBody @Valid UserEditDTO dto) {
        userService.editUser(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除用户", description = "批量删除用户")
    @SaCheckPermission("system:user:delete")
    @DeleteMapping
    public R<Void> delete(@RequestBody List<Long> ids) {
        userService.deleteUsers(ids);
        return R.ok();
    }

    @Operation(summary = "分配角色", description = "为用户分配角色")
    @SaCheckPermission("system:user:role")
    @PutMapping("/{id}/role")
    public R<Void> assignRole(@PathVariable Long id, @RequestBody @Valid UserRoleDTO dto) {
        userService.assignRoles(id, dto.getRoleIds());
        return R.ok();
    }

    @Operation(summary = "批量分配角色", description = "为多个用户批量分配角色")
    @SaCheckPermission("system:user:role")
    @PutMapping("/batch-role")
    public R<Void> batchAssignRole(@RequestBody @Valid BatchUserRoleDTO dto) {
        userService.batchAssignRoles(dto.getUserIds(), dto.getRoleIds());
        return R.ok();
    }

    @Operation(summary = "封禁/解封用户", description = "封禁或解封用户，封禁时强制下线")
    @SaCheckPermission("system:user:ban")
    @PutMapping("/{id}/ban")
    public R<Void> ban(@PathVariable Long id, @RequestBody @Valid UserBanDTO dto) {
        userService.updateUserStatus(id, dto.getStatus());
        // 封禁时强制下线
        if (dto.getStatus() == 1) {
            StpUtil.kickout(id);
        }
        return R.ok();
    }

    @Operation(summary = "导出用户", description = "导出用户列表到Excel")
    @SaCheckPermission("system:user:export")
    @GetMapping("/export")
    public void export(UserQueryDTO query, HttpServletResponse response) {
        userService.exportUsers(query, response);
    }

    @Operation(summary = "导入用户", description = "从Excel导入用户")
    @SaCheckPermission("system:user:import")
    @PostMapping("/import")
    public R<String> importUsers(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "updateExisting", defaultValue = "false") boolean updateExisting) {
        return R.ok(userService.importUsers(file, updateExisting));
    }

    @Operation(summary = "下载导入模板", description = "下载用户导入Excel模板")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        userService.downloadTemplate(response);
    }
}
