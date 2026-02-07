package com.campus.wall.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.user.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

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
    @SuppressWarnings("UnusedReturnValue")
    int updateCreditScore(@Param("userId") Long userId, @Param("creditScore") Integer creditScore);

    @Update("UPDATE users SET verify_status = #{verifyStatus}, verify_method = #{verifyMethod}, updated_at = NOW() WHERE id = #{userId}")
    int updateVerifyStatus(@Param("userId") Long userId, @Param("verifyStatus") Integer verifyStatus, @Param("verifyMethod") String verifyMethod);

    @Update("UPDATE users SET status = #{status}, updated_at = NOW() WHERE id = #{userId}")
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);

    @Update("UPDATE users SET dept_id = #{deptId}, updated_at = NOW() WHERE id = #{userId}")
    int updateDept(@Param("userId") Long userId, @Param("deptId") Long deptId);

    @Update("UPDATE users SET deleted = 1, deleted_at = #{deletedAt}, deleted_by = #{deletedBy}, deleted_reason = #{reason}, updated_at = NOW() WHERE id = #{userId}")
    @SuppressWarnings("UnusedReturnValue")
    int softDeleteById(@Param("userId") Long userId, @Param("deletedAt") java.time.LocalDateTime deletedAt, @Param("deletedBy") Long deletedBy, @Param("reason") String reason);

    @Insert("""
        <script>
        INSERT INTO users (username, password, nickname, email, phone, verify_status, status, credit_score)
        VALUES
        <foreach collection="users" item="item" separator=",">
          (#{item.username}, #{item.password}, #{item.nickname}, #{item.email}, #{item.phone},
           #{item.verifyStatus}, #{item.status}, #{item.creditScore})
        </foreach>
        </script>
        """)
    void batchInsert(@Param("users") List<User> users);

    @Delete("DELETE FROM users WHERE id = #{userId}")
    int hardDeleteById(@Param("userId") Long userId);

    @Select("""
        <script>
        SELECT * FROM users
        WHERE deleted = 1
        <if test="username != null and username != ''">
            AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="nickname != null and nickname != ''">
            AND nickname LIKE CONCAT('%', #{nickname}, '%')
        </if>
        <if test="phone != null and phone != ''">
            AND phone = #{phone}
        </if>
        <if test="loginDateStart != null">
            AND login_date <![CDATA[>=]]> #{loginDateStart}
        </if>
        <if test="loginDateEnd != null">
            AND login_date <![CDATA[<=]]> #{loginDateEnd}
        </if>
        <if test="lastDeletedAt != null and lastId != null">
            AND (
                deleted_at <![CDATA[<]]> #{lastDeletedAt}
                OR (deleted_at = #{lastDeletedAt} AND id <![CDATA[<]]> #{lastId})
            )
        </if>
        ORDER BY deleted_at DESC NULLS LAST, id DESC
        LIMIT #{size}
        </script>
        """)
    List<User> selectDeletedUsersAfter(@Param("username") String username,
                                       @Param("nickname") String nickname,
                                       @Param("phone") String phone,
                                       @Param("loginDateStart") LocalDateTime loginDateStart,
                                       @Param("loginDateEnd") LocalDateTime loginDateEnd,
                                       @Param("lastDeletedAt") LocalDateTime lastDeletedAt,
                                       @Param("lastId") Long lastId,
                                       @Param("size") long size);

    @Select("""
        <script>
        SELECT COUNT(1) FROM users
        WHERE deleted = 1
        <if test="username != null and username != ''">
            AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="nickname != null and nickname != ''">
            AND nickname LIKE CONCAT('%', #{nickname}, '%')
        </if>
        <if test="phone != null and phone != ''">
            AND phone = #{phone}
        </if>
        <if test="loginDateStart != null">
            AND login_date <![CDATA[>=]]> #{loginDateStart}
        </if>
        <if test="loginDateEnd != null">
            AND login_date <![CDATA[<=]]> #{loginDateEnd}
        </if>
        </script>
        """)
    long countDeletedUsers(@Param("username") String username,
                           @Param("nickname") String nickname,
                           @Param("phone") String phone,
                           @Param("loginDateStart") LocalDateTime loginDateStart,
                           @Param("loginDateEnd") LocalDateTime loginDateEnd);
}
