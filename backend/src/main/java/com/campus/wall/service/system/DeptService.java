package com.campus.wall.service.system;

import com.campus.wall.entity.system.SysDept;

import java.util.List;

public interface DeptService {

    List<SysDept> listAll();

    List<SysDept> getTree();

    SysDept getById(Long id);

    Long create(SysDept dept);

    void update(Long id, SysDept dept);

    void delete(Long id);
}
