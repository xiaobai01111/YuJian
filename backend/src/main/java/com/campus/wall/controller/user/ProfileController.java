package com.campus.wall.controller.user;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.post.PostQueryDTO;
import com.campus.wall.service.post.PostService;
import com.campus.wall.service.user.CreditService;
import com.campus.wall.service.user.UserService;
import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.user.UserDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户个人中心控制器
 */
@Tag(name = "个人中心", description = "用户个人中心接口")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class ProfileController {

    private final PostService postService;
    private final UserService userService;
    private final CreditService creditService;

    @Operation(summary = "获取用户信息")
    @GetMapping("/{id}")
    public R<UserDetailVO> getUserInfo(@PathVariable Long id) {
        return R.ok(userService.getUserDetail(id));
    }

    @Operation(summary = "获取用户发布的帖子")
    @GetMapping("/{id}/posts")
    public R<PageResult<PostVO>> getUserPosts(
            @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        PostQueryDTO query = new PostQueryDTO();
        query.setUserId(id);
        query.setPage(page);
        query.setSize(size);
        return R.ok(postService.queryPosts(query));
    }

    @Operation(summary = "获取我的收藏")
    @SaCheckLogin
    @GetMapping("/bookmarks")
    public R<PageResult<PostVO>> getMyBookmarks(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(postService.getUserBookmarks(userId, page, size));
    }

    @Operation(summary = "获取我的信用分")
    @SaCheckLogin
    @GetMapping("/credit")
    public R<Integer> getMyCreditScore() {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(creditService.getCreditScore(userId));
    }

    @Operation(summary = "获取我的个人信息")
    @SaCheckLogin
    @GetMapping("/me")
    public R<UserDetailVO> getMyInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(userService.getUserDetail(userId));
    }
}
