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
     * 分页查询角色
     */
    PageResult<RoleVO> queryRoles(int page, int size);

    /**
     * 创建角色
     */
    Long createRole(String roleName, String roleKey, List<Long> menuIds);

    /**
     * 更新角色
     */
    void updateRole(Long roleId, String roleName, List<Long> menuIds);

    /**
     * 删除角色
     */
    void deleteRole(Long roleId);

    /**
     * 分配角色菜单
     */
    void assignMenus(Long roleId, List<Long> menuIds);
}
