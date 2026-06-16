package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.ApiPermissionDTO;
import com.campus.wall.entity.system.SysApiPermission;

public interface ApiPermissionService {

    PageResult<SysApiPermission> query(int page, int size, String keyword, Boolean status);

    SysApiPermission create(ApiPermissionDTO dto);

    SysApiPermission update(Long id, ApiPermissionDTO dto);

    void delete(Long id);

    void refreshCache();
}
