package com.campus.wall.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.common.BusinessException;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.service.system.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper deptMapper;

    @Override
    public List<SysDept> listAll() {
        return deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSortOrder)
                .orderByAsc(SysDept::getId)
        );
    }

    @Override
    public List<SysDept> getTree() {
        List<SysDept> allDepts = listAll();
        return buildTree(allDepts, 0L);
    }

    private List<SysDept> buildTree(List<SysDept> depts, Long parentId) {
        List<SysDept> result = new ArrayList<>();
        for (SysDept dept : depts) {
            if (dept.getParentId().equals(parentId)) {
                result.add(dept);
            }
        }
        return result;
    }

    @Override
    public SysDept getById(Long id) {
        return deptMapper.selectById(id);
    }

    @Override
    @Transactional
    public Long create(SysDept dept) {
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(0);
        }
        deptMapper.insert(dept);
        return dept.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SysDept dept) {
        SysDept existing = deptMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("部门不存在");
        }
        
        existing.setDeptName(dept.getDeptName());
        existing.setParentId(dept.getParentId());
        existing.setSortOrder(dept.getSortOrder());
        existing.setLeader(dept.getLeader());
        existing.setPhone(dept.getPhone());
        existing.setEmail(dept.getEmail());
        existing.setStatus(dept.getStatus());
        
        deptMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 检查是否有子部门
        Long childCount = deptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id)
        );
        if (childCount > 0) {
            throw new BusinessException("存在子部门，无法删除");
        }
        
        deptMapper.deleteById(id);
    }
}
