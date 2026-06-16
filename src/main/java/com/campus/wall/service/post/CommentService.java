package com.campus.wall.service.post;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.post.CommentBatchDeleteDTO;
import com.campus.wall.dto.post.CommentCreateDTO;
import com.campus.wall.dto.post.CommentQueryDTO;
import com.campus.wall.dto.post.CommentUpdateDTO;
import com.campus.wall.vo.post.CommentConsoleVO;
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
     * 管理端删除评论
     */
    void deleteCommentByAdmin(Long commentId, String reason);

    /**
     * 管理端批量删除评论
     */
    void deleteCommentsByAdmin(CommentBatchDeleteDTO dto);

    /**
     * 管理端修改评论
     */
    void updateCommentByAdmin(Long commentId, CommentUpdateDTO dto, String reason);

    /**
     * 回收站恢复评论
     */
    void restoreCommentByAdmin(Long commentId, String reason);

    /**
     * 回收站彻底删除评论
     */
    void purgeCommentByAdmin(Long commentId, String reason);

    /**
     * 获取帖子的评论列表（树形结构）
     */
    List<CommentVO> getPostComments(Long postId);

    /**
     * 分页获取帖子评论
     */
    PageResult<CommentVO> getPostCommentsPage(Long postId, int page, int size);

    /**
     * 控制台评论列表
     */
    PageResult<CommentConsoleVO> queryCommentsForConsole(CommentQueryDTO query);
}
