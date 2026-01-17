package com.campus.wall.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.user.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE id = #{userId} AND deleted = 1")
    User selectDeletedById(@Param("userId") Long userId);

    @Update("UPDATE users SET deleted = 0, deleted_at = NULL, deleted_by = NULL, deleted_reason = NULL, updated_at = NOW() WHERE id = #{userId}")
    int restoreById(@Param("userId") Long userId);

    @Update("UPDATE users SET credit_score = #{creditScore}, updated_at = NOW() WHERE id = #{userId}")
    int updateCreditScore(@Param("userId") Long userId, @Param("creditScore") Integer creditScore);

    @Update("UPDATE users SET verify_status = #{verifyStatus}, verify_method = #{verifyMethod}, updated_at = NOW() WHERE id = #{userId}")
    int updateVerifyStatus(@Param("userId") Long userId, @Param("verifyStatus") Integer verifyStatus, @Param("verifyMethod") String verifyMethod);

    @Update("UPDATE users SET status = #{status}, updated_at = NOW() WHERE id = #{userId}")
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);
}
