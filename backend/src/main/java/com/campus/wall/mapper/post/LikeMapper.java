package com.campus.wall.mapper.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.post.Like;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞 Mapper
 */
@Mapper
public interface LikeMapper extends BaseMapper<Like> {
}
