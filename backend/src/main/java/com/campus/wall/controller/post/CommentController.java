package com.campus.wall.controller.post;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.post.CommentCreateDTO;
import com.campus.wall.service.post.CommentService;
import com.campus.wall.vo.post.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评论控制器
 */
@Tag(name = "评论管理", description = "评论 CRUD 接口")
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "获取帖子评论列表")
    @GetMapping("/post/{postId}")
    public R<List<CommentVO>> getPostComments(@PathVariable Long postId) {
        return R.ok(commentService.getPostComments(postId));
    }

    @Operation(summary = "分页获取帖子评论")
    @GetMapping("/post/{postId}/page")
    public R<PageResult<CommentVO>> getPostCommentsPage(
            @PathVariable Long postId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(commentService.getPostCommentsPage(postId, page, size));
    }

    @Operation(summary = "创建评论")
    @SaCheckLogin
    @PostMapping
    public R<Long> create(@Valid @RequestBody CommentCreateDTO dto) {
        return R.ok(commentService.createComment(dto));
    }

    @Operation(summary = "删除评论")
    @SaCheckLogin
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return R.ok();
    }
}
