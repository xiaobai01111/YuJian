package com.campus.wall.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.ApiPermissionDTO;
import com.campus.wall.entity.system.SysApiPermission;
import com.campus.wall.mapper.system.SysApiPermissionMapper;
import com.campus.wall.service.system.ApiPermissionService;
import com.campus.wall.service.system.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ApiPermissionServiceImpl implements ApiPermissionService {

    private final SysApiPermissionMapper apiPermissionMapper;
    private final PermissionService permissionService;

    @Override
    public PageResult<SysApiPermission> query(int page, int size, String keyword, Boolean status) {
        LambdaQueryWrapper<SysApiPermission> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            wrapper.and(w -> w.like(SysApiPermission::getUrl, like)
                    .or()
                    .like(SysApiPermission::getPermission, like)
                    .or()
                    .like(SysApiPermission::getDescription, like));
        }
        if (status != null) {
            wrapper.eq(SysApiPermission::getStatus, status);
        }
        wrapper.orderByDesc(SysApiPermission::getUpdatedAt).orderByDesc(SysApiPermission::getId);
        Page<SysApiPermission> result = apiPermissionMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public SysApiPermission create(ApiPermissionDTO dto) {
        SysApiPermission entity = new SysApiPermission();
        fill(entity, dto);
        apiPermissionMapper.insert(entity);
        permissionService.refreshCache();
        return entity;
    }

    @Override
    public SysApiPermission update(Long id, ApiPermissionDTO dto) {
        SysApiPermission entity = apiPermissionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        fill(entity, dto);
        apiPermissionMapper.updateById(entity);
        permissionService.refreshCache();
        return entity;
    }

    @Override
    public void delete(Long id) {
        apiPermissionMapper.deleteById(id);
        permissionService.refreshCache();
    }

    @Override
    public void refreshCache() {
        permissionService.refreshCache();
    }

    private void fill(SysApiPermission entity, ApiPermissionDTO dto) {
        entity.setUrl(dto.getUrl().trim());
        String method = StringUtils.hasText(dto.getHttpMethod()) ? dto.getHttpMethod().trim().toUpperCase() : "*";
        entity.setHttpMethod(method);
        entity.setPermission(dto.getPermission().trim());
        entity.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
        entity.setStatus(dto.getStatus() == null || dto.getStatus());
    }
}
