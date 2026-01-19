package com.campus.wall.controller.console;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.post.CommentQueryDTO;
import com.campus.wall.dto.post.PostQueryDTO;
import com.campus.wall.common.BusinessException;
import com.campus.wall.service.post.CommentService;
import com.campus.wall.service.post.PostService;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.service.system.ReportService;
import com.campus.wall.vo.post.CommentConsoleVO;
import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.system.ReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/console/recycle")
@RequiredArgsConstructor
public class RecycleConsoleController {

    private final PostService postService;
    private final CommentService commentService;
    private final ReportService reportService;
    private final DataScopeService dataScopeService;

    @SaCheckPermission("content:recycle:post:list")
    @GetMapping("/posts")
    public R<PageResult<PostVO>> listDeletedPosts(@Validated PostQueryDTO query) {
        assertRecycleScope();
        query.setStatus(2);
        return R.ok(postService.queryPostsForConsole(query));
    }

    @SaCheckPermission("content:recycle:post:restore")
    @PutMapping("/posts/{id}/restore")
    public R<Void> restorePost(@PathVariable Long id,
                               @RequestParam(value = "reason", required = false) String reason) {
        assertRecycleScope();
        postService.restorePostByAdmin(id, reason);
        return R.ok();
    }

    @SaCheckPermission("content:recycle:post:purge")
    @DeleteMapping("/posts/{id}")
    public R<Void> purgePost(@PathVariable Long id,
                             @RequestParam(value = "reason", required = false) String reason) {
        assertRecycleScope();
        postService.purgePostByAdmin(id, reason);
        return R.ok();
    }

    @SaCheckPermission("content:recycle:comment:list")
    @GetMapping("/comments")
    public R<PageResult<CommentConsoleVO>> listDeletedComments(@Validated CommentQueryDTO query) {
        assertRecycleScope();
        query.setStatus(1);
        return R.ok(commentService.queryCommentsForConsole(query));
    }

    @SaCheckPermission("content:recycle:comment:restore")
    @PutMapping("/comments/{id}/restore")
    public R<Void> restoreComment(@PathVariable Long id,
                                  @RequestParam(value = "reason", required = false) String reason) {
        assertRecycleScope();
        commentService.restoreCommentByAdmin(id, reason);
        return R.ok();
    }

    @SaCheckPermission("content:recycle:comment:purge")
    @DeleteMapping("/comments/{id}")
    public R<Void> purgeComment(@PathVariable Long id,
                                @RequestParam(value = "reason", required = false) String reason) {
        assertRecycleScope();
        commentService.purgeCommentByAdmin(id, reason);
        return R.ok();
    }

    @SaCheckPermission("content:recycle:report:list")
    @GetMapping("/reports")
    public R<PageResult<ReportVO>> listDeletedReports(@RequestParam(required = false) Integer status,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        assertRecycleScope();
        return R.ok(reportService.queryDeletedReports(status, page, size));
    }

    @SaCheckPermission("content:recycle:report:restore")
    @PutMapping("/reports/{id}/restore")
    public R<Void> restoreReport(@PathVariable Long id,
                                 @RequestParam(value = "reason", required = false) String reason) {
        assertRecycleScope();
        reportService.restoreReportByAdmin(id, reason);
        return R.ok();
    }

    @SaCheckPermission("content:recycle:report:purge")
    @DeleteMapping("/reports/{id}")
    public R<Void> purgeReport(@PathVariable Long id,
                               @RequestParam(value = "reason", required = false) String reason) {
        assertRecycleScope();
        reportService.purgeReportByAdmin(id, reason);
        return R.ok();
    }

    private void assertRecycleScope() {
        Long userId = StpUtil.getLoginIdAsLong();
        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        if (!scope.isAllowAll() && scope.isAllowSelf() && scope.getScopedDeptIds().isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅允许部门管理员访问回收站");
        }
    }
}
