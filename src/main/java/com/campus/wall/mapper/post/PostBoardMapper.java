package com.campus.wall.mapper.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.post.PostBoard;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子板块关联 Mapper
 */
@Mapper
public interface PostBoardMapper extends BaseMapper<PostBoard> {
}
