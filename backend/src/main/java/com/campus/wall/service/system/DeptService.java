package com.campus.wall.service.system;

import com.campus.wall.dto.system.DeptDeleteDTO;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.vo.system.DeptTreeVO;
import com.campus.wall.vo.user.UserVO;

import java.util.List;

public interface DeptService {

    List<SysDept> listAll();

    List<SysDept> getTree();

    List<DeptTreeVO> getDeptTree();

    SysDept getById(Long id);

    Long create(SysDept dept);

    void update(Long id, SysDept dept);

    /**
     * 更新部门状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 删除部门（带用户处理策略）
     */
    void delete(Long id, DeptDeleteDTO dto);

    /**
     * 获取部门下的用户列表
     */
    List<UserVO> getDeptUsers(Long deptId);

    /**
     * 获取部门下的用户数量
     */
    Long getDeptUserCount(Long deptId);
}
