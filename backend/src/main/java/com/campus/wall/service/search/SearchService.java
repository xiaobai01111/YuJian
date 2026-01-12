package com.campus.wall.service.search;

import com.campus.wall.common.PageResult;
import com.campus.wall.vo.post.PostVO;

/**
 * 搜索服务抽象接口
 * 支持 CQRS 切换能力，可替换为 Elasticsearch 等实现
 */
public interface SearchService {

    /**
     * 搜索帖子
     * @param keyword 搜索关键词
     * @param board 板块过滤（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 搜索结果
     */
    PageResult<PostVO> search(String keyword, String board, int page, int size);

    /**
     * 索引帖子（创建或更新）
     * @param postId 帖子ID
     */
    void indexPost(Long postId);

    /**
     * 删除帖子索引
     * @param postId 帖子ID
     */
    void removeIndex(Long postId);

    /**
     * 重建所有索引
     */
    void rebuildIndex();
}
