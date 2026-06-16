package com.campus.wall.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.BlocklistBatchImportDTO;
import com.campus.wall.dto.system.BlocklistDTO;
import com.campus.wall.dto.system.BlocklistQueryDTO;
import com.campus.wall.entity.system.SysBlocklist;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysBlocklistMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.BlocklistService;
import com.campus.wall.util.SecurityUtil;
import com.campus.wall.vo.system.BlocklistBatchImportResult;
import com.campus.wall.vo.system.BlocklistVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 阻止名单服务
 */
@Service
@RequiredArgsConstructor
public class BlocklistServiceImpl implements BlocklistService {

    private static final Set<String> ALLOWED_TYPES = Set.of("IP", "USER", "DEVICE");
    private static final int MAX_BATCH_SIZE = 500;

    private final SysBlocklistMapper blocklistMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<BlocklistVO> queryBlocklist(BlocklistQueryDTO query) {
        LambdaQueryWrapper<SysBlocklist> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(SysBlocklist::getCreatedAt).orderByDesc(SysBlocklist::getId);

        Page<SysBlocklist> page = blocklistMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()), wrapper
        );

        List<SysBlocklist> records = page.getRecords();
        Map<Long, String> userNameMap = resolveUserNames(records);

        List<BlocklistVO> voList = records.stream()
            .map(entity -> toVO(entity, userNameMap))
            .collect(Collectors.toList());

        return PageResult.of(voList, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    public BlocklistVO create(BlocklistDTO dto) {
        SysBlocklist entity = new SysBlocklist();
        fillEntity(entity, dto);
        entity.setCreatedBy(SecurityUtil.getCurrentUserId());

        if (existsTarget(entity.getTargetType(), entity.getTargetValue(), null)) {
            throw new BusinessException("目标已存在");
        }

        blocklistMapper.insert(entity);
        return toVO(entity, resolveUserNames(List.of(entity)));
    }

    @Override
    public BlocklistVO update(Long id, BlocklistDTO dto) {
        SysBlocklist entity = blocklistMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("阻止名单不存在");
        }

        fillEntity(entity, dto);
        entity.setId(id);
        entity.setUpdatedBy(SecurityUtil.getCurrentUserId());

        if (existsTarget(entity.getTargetType(), entity.getTargetValue(), id)) {
            throw new BusinessException("目标已存在");
        }

        blocklistMapper.updateById(entity);
        return toVO(entity, resolveUserNames(List.of(entity)));
    }

    @Override
    public void delete(Long id) {
        blocklistMapper.deleteById(id);
    }

    @Override
    public BlocklistBatchImportResult importBatch(BlocklistBatchImportDTO dto) {
        if (dto.getValues() == null || dto.getValues().isEmpty()) {
            throw new BusinessException("目标值列表不能为空");
        }
        if (dto.getValues().size() > MAX_BATCH_SIZE) {
            throw new BusinessException("单次导入不能超过" + MAX_BATCH_SIZE + "条");
        }

        String type = normalizeType(dto.getTargetType());
        if (!StringUtils.hasText(type) || !ALLOWED_TYPES.contains(type)) {
            throw new BusinessException("类型不合法");
        }

        Set<String> existing = loadExistingTargets(type);
        List<String> added = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> invalid = new ArrayList<>();

        for (String rawLine : dto.getValues()) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            ParsedLine parsed = parseLine(line);
            if (!StringUtils.hasText(parsed.value)) {
                invalid.add(line);
                continue;
            }

            String targetValue = normalizeTargetValue(type, parsed.value);
            if (!StringUtils.hasText(targetValue)) {
                invalid.add(line);
                continue;
            }
            Set<String> candidates = buildTargetCandidates(type, parsed.value);
            boolean duplicated = candidates.stream().anyMatch(existing::contains);
            if (duplicated) {
                skipped.add(targetValue);
                continue;
            }

            SysBlocklist entity = new SysBlocklist();
            entity.setTargetType(type);
            entity.setTargetValue(targetValue);
            entity.setReason(StringUtils.hasText(parsed.reason) ? parsed.reason : normalizeReason(dto.getReason()));
            entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
            entity.setExpireAt(dto.getExpireAt());
            entity.setCreatedBy(SecurityUtil.getCurrentUserId());
            blocklistMapper.insert(entity);
            existing.addAll(candidates);
            added.add(targetValue);
        }

        BlocklistBatchImportResult result = new BlocklistBatchImportResult();
        result.setAddedCount(added.size());
        result.setSkippedCount(skipped.size());
        result.setInvalidCount(invalid.size());
        result.setAdded(added);
        result.setSkipped(skipped);
        result.setInvalid(invalid);
        return result;
    }

    @Override
    public boolean isBlocked(String targetType, String targetValue) {
        String type = normalizeType(targetType);
        if (!StringUtils.hasText(type) || !ALLOWED_TYPES.contains(type) || !StringUtils.hasText(targetValue)) {
            return false;
        }

        Set<String> candidates = buildTargetCandidates(type, targetValue);
        if (candidates.isEmpty()) {
            return false;
        }

        LambdaQueryWrapper<SysBlocklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysBlocklist::getTargetType, type)
            .in(SysBlocklist::getTargetValue, candidates)
            .eq(SysBlocklist::getStatus, 0)
            .and(w -> w.isNull(SysBlocklist::getExpireAt)
                .or()
                .gt(SysBlocklist::getExpireAt, LocalDateTime.now()));
        return blocklistMapper.selectCount(wrapper) > 0;
    }

    private LambdaQueryWrapper<SysBlocklist> buildQueryWrapper(BlocklistQueryDTO query) {
        LambdaQueryWrapper<SysBlocklist> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getTargetType())) {
            wrapper.eq(SysBlocklist::getTargetType, query.getTargetType().trim().toUpperCase());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysBlocklist::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysBlocklist::getTargetValue, keyword)
                .or()
                .like(SysBlocklist::getReason, keyword));
        }
        return wrapper;
    }

    private void fillEntity(SysBlocklist entity, BlocklistDTO dto) {
        String type = normalizeType(dto.getTargetType());
        String value = normalizeTargetValue(type, dto.getTargetValue());
        if (!StringUtils.hasText(type) || !ALLOWED_TYPES.contains(type)) {
            throw new BusinessException("类型不合法");
        }
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("目标值不能为空");
        }
        entity.setTargetType(type);
        entity.setTargetValue(value);
        entity.setReason(normalizeReason(dto.getReason()));
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        entity.setExpireAt(dto.getExpireAt());
    }

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return type.trim().toUpperCase();
    }

    private boolean existsTarget(String targetType, String targetValue, Long excludeId) {
        Set<String> candidates = buildTargetCandidates(targetType, targetValue);
        if (candidates.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<SysBlocklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysBlocklist::getTargetType, targetType)
            .in(SysBlocklist::getTargetValue, candidates);
        if (excludeId != null) {
            wrapper.ne(SysBlocklist::getId, excludeId);
        }
        return blocklistMapper.selectCount(wrapper) > 0;
    }

    private Set<String> loadExistingTargets(String targetType) {
        LambdaQueryWrapper<SysBlocklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysBlocklist::getTargetValue)
            .eq(SysBlocklist::getTargetType, targetType);
        List<SysBlocklist> list = blocklistMapper.selectList(wrapper);
        Set<String> result = new HashSet<>();
        for (SysBlocklist item : list) {
            if (StringUtils.hasText(item.getTargetValue())) {
                result.add(item.getTargetValue());
                String normalized = normalizeTargetValue(targetType, item.getTargetValue());
                if (StringUtils.hasText(normalized)) {
                    result.add(normalized);
                }
            }
        }
        return result;
    }

    private ParsedLine parseLine(String line) {
        String[] parts = line.split("\\|", 2);
        String value = parts[0].trim();
        String reason = parts.length > 1 ? parts[1].trim() : null;
        return new ParsedLine(value, reason);
    }

    private Set<String> buildTargetCandidates(String type, String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptySet();
        }
        Set<String> candidates = new LinkedHashSet<>();
        String trimmed = value.trim();
        if (StringUtils.hasText(trimmed)) {
            candidates.add(trimmed);
        }
        String normalized = normalizeTargetValue(type, value);
        if (StringUtils.hasText(normalized)) {
            candidates.add(normalized);
        }
        return candidates;
    }

    private String normalizeTargetValue(String type, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalizedType = normalizeType(type);
        if (!StringUtils.hasText(normalizedType)) {
            return value.trim();
        }

        String trimmed = value.trim();
        return switch (normalizedType) {
            case "IP" -> normalizeIp(trimmed);
            case "USER" -> normalizeUserId(trimmed);
            default -> trimmed;
        };
    }

    private String normalizeUserId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.toString(Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            return value.trim();
        }
    }

    private String normalizeIp(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.contains(",")) {
            normalized = normalized.split(",")[0].trim();
        }
        if (normalized.startsWith("[") && normalized.contains("]")) {
            normalized = normalized.substring(1, normalized.indexOf(']'));
        } else {
            int colonIndex = normalized.indexOf(':');
            if (colonIndex > -1 && normalized.indexOf(':', colonIndex + 1) == -1) {
                normalized = normalized.substring(0, colonIndex);
            }
        }
        if (normalized.startsWith("::ffff:")) {
            normalized = normalized.substring(7);
        }
        try {
            return InetAddress.getByName(normalized).getHostAddress();
        } catch (Exception ignored) {
            return normalized;
        }
    }

    private String normalizeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<Long, String> resolveUserNames(List<SysBlocklist> records) {
        Set<Long> userIds = records.stream()
            .map(SysBlocklist::getCreatedBy)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> map = new HashMap<>();
        for (User user : users) {
            String name = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
            map.put(user.getId(), name);
        }
        return map;
    }

    private BlocklistVO toVO(SysBlocklist entity, Map<Long, String> userNameMap) {
        BlocklistVO vo = new BlocklistVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setCreatedByName(userNameMap.get(entity.getCreatedBy()));
        vo.setExpired(isExpired(entity.getExpireAt()));
        return vo;
    }

    private boolean isExpired(LocalDateTime expireAt) {
        return expireAt != null && expireAt.isBefore(LocalDateTime.now());
    }

    private record ParsedLine(String value, String reason) {
    }
}
