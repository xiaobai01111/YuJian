package com.campus.wall.service.user;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.user.UserQueryDTO;
import com.campus.wall.dto.user.UserUpdateDTO;
import com.campus.wall.vo.user.UserDetailVO;
import com.campus.wall.vo.user.UserVO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 分页查询用户
     */
    PageResult<UserVO> queryUsers(UserQueryDTO query);

    /**
     * 获取用户详情
     */
    UserDetailVO getUserDetail(Long userId);

    /**
     * 更新用户信息
     */
    void updateUser(Long userId, UserUpdateDTO dto);

    /**
     * 封禁/解封用户
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 分配用户角色
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 更新用户信用分
     */
    void updateCreditScore(Long userId, int delta, String reason);
}
