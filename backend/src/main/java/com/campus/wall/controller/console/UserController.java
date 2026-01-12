package com.campus.wall.controller.console;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.user.UserBanDTO;
import com.campus.wall.dto.user.UserQueryDTO;
import com.campus.wall.dto.user.UserRoleDTO;
import com.campus.wall.service.user.UserService;
import com.campus.wall.vo.user.UserDetailVO;
import com.campus.wall.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "分配角色", description = "为用户分配角色")
    @SaCheckPermission("system:user:edit")
    @PutMapping("/{id}/role")
    public R<Void> assignRole(@PathVariable Long id, @RequestBody @Valid UserRoleDTO dto) {
        userService.assignRoles(id, dto.getRoleIds());
        return R.ok();
    }

    @Operation(summary = "封禁/解封用户", description = "封禁或解封用户，封禁时强制下线")
    @SaCheckPermission("system:user:edit")
    @PutMapping("/{id}/ban")
    public R<Void> ban(@PathVariable Long id, @RequestBody @Valid UserBanDTO dto) {
        userService.updateUserStatus(id, dto.getStatus());
        // 封禁时强制下线
        if (dto.getStatus() == 1) {
            StpUtil.kickout(id);
        }
        return R.ok();
    }
}
