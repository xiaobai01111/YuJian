package com.campus.wall.service.user;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.user.UserCreateDTO;
import com.campus.wall.dto.user.UserEditDTO;
import com.campus.wall.dto.user.UserQueryDTO;
import com.campus.wall.dto.user.UserUpdateDTO;
import com.campus.wall.vo.user.UserDetailVO;
import com.campus.wall.vo.user.UserVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

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
     * 创建用户
     */
    Long createUser(UserCreateDTO dto);

    /**
     * 编辑用户
     */
    void editUser(Long userId, UserEditDTO dto);

    /**
     * 删除用户
     */
    void deleteUsers(List<Long> userIds);

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
     * 批量分配用户角色
     */
    void batchAssignRoles(List<Long> userIds, List<Long> roleIds);

    /**
     * 更新用户信用分
     */
    void updateCreditScore(Long userId, int delta, String reason);

    /**
     * 导出用户
     */
    void exportUsers(UserQueryDTO query, HttpServletResponse response);

    /**
     * 导入用户
     */
    String importUsers(MultipartFile file, boolean updateExisting);

    /**
     * 下载导入模板
     */
    void downloadTemplate(HttpServletResponse response);
}
