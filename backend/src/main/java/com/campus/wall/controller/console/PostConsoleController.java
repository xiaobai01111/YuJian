package com.campus.wall.controller.console;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@Tag(name = "控制台帖子管理", description = "控制台帖子查询")
@RestController
@RequestMapping("/api/v1/console/posts")
@RequiredArgsConstructor
public class PostConsoleController {

    private final PostService postService;

    @Operation(summary = "控制台帖子列表", description = "按数据权限查询帖子列表")
    @GetMapping
    public R<PageResult<PostVO>> list(PostQueryDTO query) {
        return R.ok(postService.queryPostsForConsole(query));
    }

    @Operation(summary = "控制台创建帖子")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PostCreateDTO dto) {
        return R.ok(postService.createPostByAdmin(dto));
    }

    @Operation(summary = "控制台编辑帖子")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody PostUpdateDTO dto,
                          @RequestParam(value = "reason", required = false) String reason) {
        postService.updatePostByAdmin(id, dto, reason);
        return R.ok();
    }

    @Operation(summary = "控制台删除帖子")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id,
                          @RequestParam(value = "reason", required = false) String reason) {
        postService.deletePostByAdmin(id, reason);
        return R.ok();
    }

    @Operation(summary = "控制台标记已解决")
    @PutMapping("/{id}/resolve")
    public R<Void> resolve(@PathVariable Long id,
                           @RequestParam(value = "reason", required = false) String reason) {
        postService.markAsResolvedByAdmin(id, reason);
        return R.ok();
    }

    @Operation(summary = "控制台标记已售出")
    @PutMapping("/{id}/sold")
    public R<Void> sold(@PathVariable Long id,
                        @RequestParam(value = "reason", required = false) String reason) {
        postService.markAsSoldByAdmin(id, reason);
        return R.ok();
    }
}
