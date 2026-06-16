package com.campus.wall.controller.search;

import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.service.search.SearchService;
import com.campus.wall.vo.post.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索控制器
 */
@Tag(name = "搜索", description = "全文搜索接口")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "搜索帖子")
    @GetMapping("/posts")
    public R<PageResult<PostVO>> searchPosts(
            @Parameter(description = "搜索关键词", required = true) @RequestParam String keyword,
            @Parameter(description = "板块过滤") @RequestParam(required = false) String board,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(searchService.search(keyword, board, page, size));
    }
}
