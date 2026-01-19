package com.campus.wall.controller.console;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.post.CommentQueryDTO;
import com.campus.wall.service.post.CommentService;
import com.campus.wall.vo.post.CommentConsoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/console/comments")
@RequiredArgsConstructor
public class CommentConsoleController {

    private final CommentService commentService;

    @SaCheckPermission("content:comment:list")
    @GetMapping
    public R<PageResult<CommentConsoleVO>> list(@Validated CommentQueryDTO query) {
        return R.ok(commentService.queryCommentsForConsole(query));
    }

    @SaCheckPermission("content:comment:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        commentService.deleteCommentByAdmin(id);
        return R.ok();
    }
}
