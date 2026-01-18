package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.vo.system.RoleVO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService {

    /**
     * 获取所有角色
     */
    List<RoleVO> getAllRoles();

    /**
     * 获取角色菜单ID列表
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 分页查询角色
     */
    PageResult<RoleVO> queryRoles(int page, int size);

    /**
     * 创建角色
     */
    RoleVO createRole(String roleName, String roleKey, Integer status, Integer sortOrder, String remark, List<Long> menuIds);

    /**
     * 更新角色
     */
    RoleVO updateRole(Long roleId, String roleName, String roleKey, Integer status, Integer sortOrder, String remark, List<Long> menuIds);

    /**
     * 删除角色
     */
    void deleteRole(Long roleId, boolean deleteUsers, String reason);

    /**
     * 分配角色菜单
     */
    RoleVO assignMenus(Long roleId, List<Long> menuIds);

    /**
     * 获取角色数据权限部门
     */
    List<Long> getRoleDeptIds(Long roleId);

    /**
     * 分配角色数据权限
     */
    RoleVO assignDepts(Long roleId, List<Long> deptIds);

    /**
     * 获取角色下的用户列表
     */
    List<com.campus.wall.vo.user.UserVO> getRoleUsers(Long roleId);
}
