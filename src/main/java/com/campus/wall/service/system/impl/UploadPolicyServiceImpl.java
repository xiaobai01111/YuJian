package com.campus.wall.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.UploadPolicyUpdateDTO;
import com.campus.wall.entity.system.SysUploadPolicy;
import com.campus.wall.enums.file.FileVisibility;
import com.campus.wall.enums.file.FileTargetType;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.mapper.system.SysUploadPolicyMapper;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.service.system.UploadPolicyService;
import com.campus.wall.vo.system.UploadPolicyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UploadPolicyServiceImpl implements UploadPolicyService {

    private static final Set<String> SUPPORTED_ASSET_TYPES = Set.of("file", "gallery", "resource");
    private static final String ID_CARD_SCENE_CODE = "id_card";
    private static final String ID_CARD_TARGET_TYPE = FileTargetType.ID_CARD.getCode();
    private static final String ID_CARD_LOCKED_ASSET_TYPE = "resource";
    private static final String ID_CARD_LOCKED_VISIBILITY = "PRIVATE";
    private final SysUploadPolicyMapper uploadPolicyMapper;
    private final FileRecordMapper fileRecordMapper;
    private final OperLogService operLogService;

    @Override
    public List<UploadPolicyVO> listPolicies() {
        ensureDefaultPolicies();
        List<SysUploadPolicy> policies = uploadPolicyMapper.selectList(
            new LambdaQueryWrapper<SysUploadPolicy>().orderByAsc(SysUploadPolicy::getId)
        );
        return policies.stream().map(this::toVO).toList();
    }

    @Override
    public UploadPolicyVO updatePolicy(String sceneCode, UploadPolicyUpdateDTO dto) {
        if (!StringUtils.hasText(sceneCode)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "sceneCode不能为空");
        }
        String normalizedSceneCode = sceneCode.trim().toLowerCase(Locale.ROOT);
        SysUploadPolicy policy = uploadPolicyMapper.selectOne(
            new LambdaQueryWrapper<SysUploadPolicy>().eq(SysUploadPolicy::getSceneCode, normalizedSceneCode)
        );
        if (policy == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "上传策略不存在");
        }
        if (isIdCardScene(policy.getSceneCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "身份材料策略不允许修改");
        }
        String assetType = normalizeAssetType(dto.getAssetType());
        if (assetType == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "assetType不合法");
        }
        Map<String, Object> before = toAuditSnapshot(policy);
        policy.setAssetType(assetType);
        policy.setVisibility(normalizeVisibility(dto.getVisibility()));
        policy.setUpdatedAt(LocalDateTime.now());
        uploadPolicyMapper.updateById(policy);
        if (policy.getVisibility() != null && policy.getSceneCode() != null
                && policy.getSceneCode().equalsIgnoreCase(policy.getAssetType())) {
            fileRecordMapper.update(null, new LambdaUpdateWrapper<FileRecord>()
                    .eq(FileRecord::getAssetType, policy.getAssetType())
                    .ne(FileRecord::getTargetType, ID_CARD_TARGET_TYPE)
                    .set(FileRecord::getVisibility, policy.getVisibility()));
        }
        enforceIdCardFileVisibility();
        Map<String, Object> after = toAuditSnapshot(policy);
        operLogService.log(resolveOperatorId(), null, "upload_policy", policy.getId(),
            "update", null, before, after, null);
        return toVO(policy);
    }

    @Override
    public SysUploadPolicy findByScene(String sceneCode) {
        if (!StringUtils.hasText(sceneCode)) {
            return null;
        }
        String normalizedSceneCode = sceneCode.trim().toLowerCase(Locale.ROOT);
        SysUploadPolicy policy = uploadPolicyMapper.selectOne(
            new LambdaQueryWrapper<SysUploadPolicy>().eq(SysUploadPolicy::getSceneCode, normalizedSceneCode)
        );
        if (isIdCardScene(normalizedSceneCode)) {
            return enforceIdCardPolicy(policy);
        }
        return policy;
    }

    private UploadPolicyVO toVO(SysUploadPolicy policy) {
        if (policy == null) {
            return null;
        }
        UploadPolicyVO vo = new UploadPolicyVO();
        vo.setSceneCode(policy.getSceneCode());
        vo.setSceneName(policy.getSceneName());
        vo.setAssetType(policy.getAssetType());
        vo.setVisibility(policy.getVisibility());
        vo.setUpdatedAt(policy.getUpdatedAt());
        return vo;
    }

    private String normalizeAssetType(String assetType) {
        if (!StringUtils.hasText(assetType)) {
            return null;
        }
        String normalized = assetType.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_ASSET_TYPES.contains(normalized) ? normalized : null;
    }

    private String normalizeVisibility(String visibility) {
        if (!StringUtils.hasText(visibility)) {
            return null;
        }
        for (FileVisibility item : FileVisibility.values()) {
            if (item.getCode().equalsIgnoreCase(visibility.trim())) {
                return item.getCode();
            }
        }
        throw new BusinessException(ResultCode.BAD_REQUEST, "visibility不合法");
    }

    private void ensureDefaultPolicies() {
        List<SysUploadPolicy> existing = uploadPolicyMapper.selectList(
            new LambdaQueryWrapper<SysUploadPolicy>().select(SysUploadPolicy::getSceneCode)
        );
        Set<String> existingCodes = existing.stream()
            .map(SysUploadPolicy::getSceneCode)
            .filter(StringUtils::hasText)
            .map(code -> code.trim().toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());

        List<SysUploadPolicy> defaults = new ArrayList<>();
        defaults.add(buildDefaultPolicy("post", "帖子图片", "gallery", null));
        defaults.add(buildDefaultPolicy("comment", "评论图片", "gallery", null));
        defaults.add(buildDefaultPolicy("avatar", "头像", "gallery", null));
        defaults.add(buildDefaultPolicy("id_card", "身份材料", "resource", "PRIVATE"));
        defaults.add(buildDefaultPolicy("public", "公共上传（前台）", "gallery", null));
        defaults.add(buildDefaultPolicy("package", "安装包", "resource", "PUBLIC"));
        defaults.add(buildDefaultPolicy("file", "业务附件库", "file", null));
        defaults.add(buildDefaultPolicy("gallery", "公共媒体库", "gallery", null));
        defaults.add(buildDefaultPolicy("resource", "系统资源库", "resource", null));
        defaults.add(buildDefaultPolicy("campus.hero", "校园展示位", "resource", null));
        defaults.add(buildDefaultPolicy("campus.school", "校园信息", "resource", null));

        LocalDateTime now = LocalDateTime.now();
        for (SysUploadPolicy policy : defaults) {
            String key = policy.getSceneCode().toLowerCase(Locale.ROOT);
            if (existingCodes.contains(key)) {
                continue;
            }
            policy.setCreatedAt(now);
            policy.setUpdatedAt(now);
            uploadPolicyMapper.insert(policy);
        }
    }

    private SysUploadPolicy buildDefaultPolicy(String sceneCode, String sceneName, String assetType, String visibility) {
        SysUploadPolicy policy = new SysUploadPolicy();
        policy.setSceneCode(sceneCode);
        policy.setSceneName(sceneName);
        policy.setAssetType(assetType);
        policy.setVisibility(visibility);
        return policy;
    }

    private Map<String, Object> toAuditSnapshot(SysUploadPolicy policy) {
        if (policy == null) {
            return Map.of();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("sceneCode", policy.getSceneCode());
        snapshot.put("sceneName", policy.getSceneName());
        snapshot.put("assetType", policy.getAssetType());
        snapshot.put("visibility", policy.getVisibility());
        return snapshot;
    }

    private Long resolveOperatorId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId == null) {
                return null;
            }
            return Long.valueOf(loginId.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isIdCardScene(String sceneCode) {
        return StringUtils.hasText(sceneCode) && ID_CARD_SCENE_CODE.equalsIgnoreCase(sceneCode.trim());
    }

    private SysUploadPolicy enforceIdCardPolicy(SysUploadPolicy policy) {
        if (policy == null) {
            return null;
        }
        boolean changed = false;
        if (!ID_CARD_LOCKED_ASSET_TYPE.equalsIgnoreCase(policy.getAssetType())) {
            policy.setAssetType(ID_CARD_LOCKED_ASSET_TYPE);
            changed = true;
        }
        if (!ID_CARD_LOCKED_VISIBILITY.equalsIgnoreCase(policy.getVisibility())) {
            policy.setVisibility(ID_CARD_LOCKED_VISIBILITY);
            changed = true;
        }
        if (changed) {
            policy.setUpdatedAt(LocalDateTime.now());
            uploadPolicyMapper.updateById(policy);
        }
        enforceIdCardFileVisibility();
        return policy;
    }

    private void enforceIdCardFileVisibility() {
        fileRecordMapper.update(null, new LambdaUpdateWrapper<FileRecord>()
            .eq(FileRecord::getTargetType, ID_CARD_TARGET_TYPE)
            .ne(FileRecord::getVisibility, ID_CARD_LOCKED_VISIBILITY)
            .set(FileRecord::getVisibility, ID_CARD_LOCKED_VISIBILITY));
    }
}
