package com.campus.wall.mapper.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.post.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
