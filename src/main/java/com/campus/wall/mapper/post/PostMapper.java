package com.campus.wall.mapper.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.entity.post.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 帖子 Mapper
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("""
        SELECT p.*, ts_rank(search_vector, plainto_tsquery('simple', #{keyword})) AS rank
        FROM posts p
        WHERE p.search_vector @@ plainto_tsquery('simple', #{keyword})
          AND p.status = 0
        ORDER BY rank DESC
        """)
    IPage<Post> fullTextSearch(Page<Post> page, @Param("keyword") String keyword);

    @Update("UPDATE posts SET view_count = view_count + 1 WHERE id = #{postId}")
    @SuppressWarnings("UnusedReturnValue")
    int incrementViewCount(@Param("postId") Long postId);

    @Update("UPDATE posts SET like_count = like_count + #{delta} WHERE id = #{postId}")
    @SuppressWarnings("UnusedReturnValue")
    int updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Update("UPDATE posts SET comment_count = comment_count + #{delta} WHERE id = #{postId}")
    @SuppressWarnings("UnusedReturnValue")
    int updateCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Update("UPDATE posts SET last_interaction_at = NOW() WHERE id = #{postId}")
    @SuppressWarnings("UnusedReturnValue")
    int updateLastInteractionAt(@Param("postId") Long postId);
}
