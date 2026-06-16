package com.campus.wall.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.user.IdentityVerification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 身份审核 Mapper
 */
@Mapper
public interface IdentityVerificationMapper extends BaseMapper<IdentityVerification> {
}
