package com.campus.wall.service.post;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.post.CommentCreateDTO;
import com.campus.wall.vo.post.CommentVO;

import java.util.List;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 创建评论
     */
    Long createComment(CommentCreateDTO dto);

    /**
     * 删除评论
     */
    void deleteComment(Long commentId);

    /**
     * 获取帖子的评论列表（树形结构）
     */
    List<CommentVO> getPostComments(Long postId);

    /**
     * 分页获取帖子评论
     */
    PageResult<CommentVO> getPostCommentsPage(Long postId, int page, int size);
}
