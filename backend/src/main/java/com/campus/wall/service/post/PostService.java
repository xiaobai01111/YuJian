package com.campus.wall.service.post;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.post.PostCreateDTO;
import com.campus.wall.dto.post.PostQueryDTO;
import com.campus.wall.dto.post.PostUpdateDTO;
import com.campus.wall.vo.post.PostVO;

/**
 * 帖子服务接口
 */
public interface PostService {

    /**
     * 创建帖子
     */
    Long createPost(PostCreateDTO dto);

    /**
     * 控制台创建帖子
     */
    Long createPostByAdmin(PostCreateDTO dto);

    /**
     * 更新帖子
     */
    void updatePost(Long postId, PostUpdateDTO dto);

    /**
     * 控制台编辑帖子
     */
    void updatePostByAdmin(Long postId, PostUpdateDTO dto);

    /**
     * 删除帖子
     */
    void deletePost(Long postId);

    /**
     * 控制台删除帖子
     */
    void deletePostByAdmin(Long postId);

    /**
     * 获取帖子详情
     */
    PostVO getPostDetail(Long postId);

    /**
     * 分页查询帖子
     */
    PageResult<PostVO> queryPosts(PostQueryDTO query);

    /**
     * 控制台分页查询帖子（应用数据权限）
     */
    PageResult<PostVO> queryPostsForConsole(PostQueryDTO query);

    /**
     * 点赞帖子
     */
    void likePost(Long postId);

    /**
     * 取消点赞
     */
    void unlikePost(Long postId);

    /**
     * 收藏帖子
     */
    void bookmarkPost(Long postId);

    /**
     * 取消收藏
     */
    void unbookmarkPost(Long postId);

    /**
     * 获取用户收藏列表
     */
    PageResult<PostVO> getUserBookmarks(Long userId, int page, int size);

    /**
     * 标记帖子为已解决
     */
    void markAsResolved(Long postId);

    /**
     * 控制台标记已解决
     */
    void markAsResolvedByAdmin(Long postId);

    /**
     * 全文搜索帖子
     */
    PageResult<PostVO> searchPosts(String keyword, int page, int size);
}
