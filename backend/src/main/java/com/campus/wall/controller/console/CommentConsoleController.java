package com.campus.wall.controller.console;

import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.post.CommentBatchDeleteDTO;
import com.campus.wall.dto.post.CommentQueryDTO;
import com.campus.wall.dto.post.CommentUpdateDTO;
import com.campus.wall.service.post.CommentService;
import com.campus.wall.vo.post.CommentConsoleVO;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/console/comments")
@RequiredArgsConstructor
public class CommentConsoleController {

    private final CommentService commentService;

    @GetMapping
    public R<PageResult<CommentConsoleVO>> list(@Validated CommentQueryDTO query) {
        return R.ok(commentService.queryCommentsForConsole(query));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id,
                          @RequestParam(value = "reason", required = false) String reason) {
        commentService.deleteCommentByAdmin(id, reason);
        return R.ok();
    }

    @PostMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody @Valid CommentBatchDeleteDTO dto) {
        commentService.deleteCommentsByAdmin(dto);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody @Valid CommentUpdateDTO dto,
                          @RequestParam(value = "reason", required = false) String reason) {
        commentService.updateCommentByAdmin(id, dto, reason);
        return R.ok();
    }
}
