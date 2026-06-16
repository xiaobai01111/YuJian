package com.campus.wall.service.system;

import com.campus.wall.dto.system.DeptDeleteDTO;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.vo.system.DeptTreeVO;
import com.campus.wall.vo.user.UserVO;
import jakarta.servlet.http.HttpServletResponse;

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
     * 调整部门层级
     */
    void move(Long id, Long parentId, Integer sortOrder);

    /**
     * 调整部门排序
     */
    void updateSort(Long id, Integer sortOrder);

    /**
     * 获取部门下的用户列表
     */
    List<UserVO> getDeptUsers(Long deptId);

    /**
     * 获取部门下的用户数量
     */
    Long getDeptUserCount(Long deptId);

    /**
     * 导出部门列表
     */
    void exportDepts(HttpServletResponse response);

    /**
     * 导入部门列表
     */
    String importDepts(org.springframework.web.multipart.MultipartFile file, boolean updateExisting);

    /**
     * 同步部门（可选）
     */
    String syncDepts();
}
