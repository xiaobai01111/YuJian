package com.campus.wall.controller.post;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.post.PostBatchBookmarkDTO;
import com.campus.wall.dto.post.PostCreateDTO;
import com.campus.wall.dto.post.PostQueryDTO;
import com.campus.wall.dto.post.PostUpdateDTO;
import com.campus.wall.dto.system.ReportBatchCreateDTO;
import com.campus.wall.dto.system.ReportCreateDTO;
import com.campus.wall.service.post.PostService;
import com.campus.wall.service.system.ReportService;
import com.campus.wall.vo.common.BatchActionResultVO;
import com.campus.wall.vo.post.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子控制器
 */
@Tag(name = "帖子管理", description = "帖子 CRUD 接口")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ReportService reportService;

    @Operation(summary = "获取帖子列表")
    @GetMapping
    public R<PageResult<PostVO>> list(PostQueryDTO query) {
        return R.ok(postService.queryPosts(query));
    }

    @Operation(summary = "获取帖子详情")
    @GetMapping("/{id}")
    public R<PostVO> detail(@PathVariable Long id) {
        return R.ok(postService.getPostDetail(id));
    }

    @Operation(summary = "记录阅读")
    @PostMapping("/{id}/view")
    public R<Void> recordView(@PathVariable Long id) {
        postService.recordPostView(id);
        return R.ok();
    }

    @Operation(summary = "创建帖子")
    @SaCheckLogin
    @PostMapping
    public R<Long> create(@Valid @RequestBody PostCreateDTO dto) {
        return R.ok(postService.createPost(dto));
    }

    @Operation(summary = "更新帖子")
    @SaCheckLogin
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PostUpdateDTO dto) {
        postService.updatePost(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除帖子")
    @SaCheckLogin
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return R.ok();
    }

    @Operation(summary = "标记已解决")
    @SaCheckLogin
    @PutMapping("/{id}/resolve")
    public R<Void> resolve(@PathVariable Long id) {
        postService.markAsResolved(id);
        return R.ok();
    }

    @Operation(summary = "点赞帖子")
    @SaCheckLogin
    @PostMapping("/{id}/like")
    public R<Void> like(@PathVariable Long id) {
        postService.likePost(id);
        return R.ok();
    }

    @Operation(summary = "取消点赞")
    @SaCheckLogin
    @DeleteMapping("/{id}/like")
    public R<Void> unlike(@PathVariable Long id) {
        postService.unlikePost(id);
        return R.ok();
    }

    @Operation(summary = "收藏帖子")
    @SaCheckLogin
    @PostMapping("/{id}/bookmark")
    public R<Void> bookmark(@PathVariable Long id) {
        postService.bookmarkPost(id);
        return R.ok();
    }

    @Operation(summary = "批量收藏帖子")
    @SaCheckLogin
    @PostMapping("/bookmarks/batch")
    public R<BatchActionResultVO> batchBookmark(@Valid @RequestBody PostBatchBookmarkDTO dto) {
        return R.ok(postService.bookmarkPosts(dto.getPostIds()));
    }

    @Operation(summary = "取消收藏")
    @SaCheckLogin
    @DeleteMapping("/{id}/bookmark")
    public R<Void> unbookmark(@PathVariable Long id) {
        postService.unbookmarkPost(id);
        return R.ok();
    }

    @Operation(summary = "获取用户收藏列表")
    @SaCheckLogin
    @GetMapping("/bookmarks")
    public R<PageResult<PostVO>> bookmarks(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(postService.getUserBookmarks(userId, page, size));
    }

    @Operation(summary = "搜索帖子")
    @GetMapping("/search")
    public R<PageResult<PostVO>> search(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(postService.searchPosts(keyword, page, size));
    }

    @Operation(summary = "举报帖子")
    @SaCheckLogin
    @PostMapping("/{id}/report")
    public R<Long> report(@PathVariable Long id, @Valid @RequestBody ReportCreateDTO dto) {
        dto.setPostId(id);
        return R.ok(reportService.createReport(dto));
    }

    @Operation(summary = "批量举报帖子")
    @SaCheckLogin
    @PostMapping("/reports/batch")
    public R<BatchActionResultVO> batchReport(@Valid @RequestBody ReportBatchCreateDTO dto) {
        return R.ok(reportService.createReports(dto));
    }
}
