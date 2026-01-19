package com.campus.wall.controller.console;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.post.PostQueryDTO;
import com.campus.wall.dto.post.PostCreateDTO;
import com.campus.wall.dto.post.PostUpdateDTO;
import com.campus.wall.service.post.PostService;
import com.campus.wall.vo.post.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

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

    @Operation(summary = "控制台创建帖子")
    @SaCheckPermission("content:post:add")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PostCreateDTO dto) {
        return R.ok(postService.createPostByAdmin(dto));
    }

    @Operation(summary = "控制台编辑帖子")
    @SaCheckPermission("content:post:edit")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PostUpdateDTO dto) {
        postService.updatePostByAdmin(id, dto);
        return R.ok();
    }

    @Operation(summary = "控制台删除帖子")
    @SaCheckPermission("content:post:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        postService.deletePostByAdmin(id);
        return R.ok();
    }

    @Operation(summary = "控制台标记已解决")
    @SaCheckPermission("content:post:resolve")
    @PutMapping("/{id}/resolve")
    public R<Void> resolve(@PathVariable Long id) {
        postService.markAsResolvedByAdmin(id);
        return R.ok();
    }
}
