package com.campus.wall.service.user;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.user.UserCreateDTO;
import com.campus.wall.dto.user.UserEditDTO;
import com.campus.wall.dto.user.UserBatchAssignDTO;
import com.campus.wall.dto.user.UserQueryDTO;
import com.campus.wall.dto.user.UserUpdateDTO;
import com.campus.wall.dto.user.UserProfileUpdateDTO;
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
     * 分页查询已删除用户
     */
    PageResult<UserVO> queryDeletedUsers(UserQueryDTO query);

    /**
     * 获取用户详情
     */
    UserDetailVO getUserDetail(Long userId);

    /**
     * 根据ID获取用户VO
     */
    UserVO getUserById(Long userId);

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
     * 删除用户（带原因）
     */
    void deleteUsersWithReason(List<Long> userIds, Long operatorId, String reason);

    /**
     * 恢复已删除用户
     */
    void restoreUser(Long userId, Long operatorId);

    /**
     * 恢复已删除用户（带理由）
     */
    void restoreUser(Long userId, Long operatorId, String reason);

    /**
     * 彻底删除已软删用户
     */
    void purgeUser(Long userId, Long operatorId, String reason);

    /**
     * 更新用户信息
     */
    void updateUser(Long userId, UserUpdateDTO dto);

    /**
     * 更新个人中心资料
     */
    void updateProfile(Long userId, UserProfileUpdateDTO dto);

    /**
     * 封禁/解封用户
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 封禁/解封用户（带理由和操作者）
     */
    void updateUserStatusWithReason(Long userId, Integer status, String reason, Long operatorId);

    /**
     * 分配用户角色
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 批量分配用户角色
     */
    void batchAssignRoles(List<Long> userIds, List<Long> roleIds);

    /**
     * 按条件批量分配角色/部门
     */
    int batchAssignByQuery(UserBatchAssignDTO dto, Long operatorId);

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
