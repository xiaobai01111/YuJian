package com.campus.wall.controller.console;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.post.PostQueryDTO;
import com.campus.wall.service.post.PostService;
import com.campus.wall.vo.post.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "控制台帖子管理", description = "控制台帖子查询")
@RestController
@RequestMapping("/api/v1/console/posts")
@RequiredArgsConstructor
public class PostConsoleController {

    private final PostService postService;

    @Operation(summary = "控制台帖子列表", description = "按数据权限查询帖子列表")
    @SaCheckPermission("content:post:list")
    @GetMapping
    public R<PageResult<PostVO>> list(PostQueryDTO query) {
        return R.ok(postService.queryPostsForConsole(query));
    }
}
