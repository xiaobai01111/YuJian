package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.OnlineUserKickoutDTO;
import com.campus.wall.dto.system.OnlineUserQueryDTO;
import com.campus.wall.service.system.OnlineUserService;
import com.campus.wall.vo.system.OnlineUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/console/online-users")
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    @SaCheckPermission("system:online:list")
    @GetMapping
    public R<PageResult<OnlineUserVO>> list(@Validated OnlineUserQueryDTO query) {
        return R.ok(onlineUserService.queryOnlineUsers(query));
    }

    @SaCheckPermission("system:online:kickout")
    @PostMapping("/kickout")
    public R<Void> kickout(@Valid @RequestBody OnlineUserKickoutDTO dto) {
        onlineUserService.kickoutByToken(dto.getToken());
        return R.ok();
    }
}
