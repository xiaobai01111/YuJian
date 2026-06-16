package com.campus.wall.vo.system;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeptTreeVO {
    private Long id;
    private Long parentId;
    private String deptName;
    private Integer sortOrder;
    private String leader;
    private String phone;
    private String email;
    private Integer status;
    private Integer dataScope;
    private LocalDateTime createdAt;
    private List<DeptTreeVO> children;
}
