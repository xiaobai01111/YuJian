package com.campus.wall.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.campus.CampusHeroDTO;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.system.CampusHero;
import com.campus.wall.entity.user.User;
import com.campus.wall.common.BusinessException;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.system.CampusHeroMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.CampusHeroService;
import com.campus.wall.vo.campus.CampusHeroVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampusHeroServiceImpl implements CampusHeroService {

    private final CampusHeroMapper campusHeroMapper;
    private final UserMapper userMapper;
    private final PostMapper postMapper;

    private static final int AVATAR_LIMIT = 4;

    @Override
    public PageResult<CampusHeroVO> query(int page, int size, String keyword, Boolean enabled) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        LambdaQueryWrapper<CampusHero> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) {
            wrapper.eq(CampusHero::getEnabled, enabled);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(CampusHero::getPageName, kw)
                .or().like(CampusHero::getTitleHighlight, kw)
                .or().like(CampusHero::getTitleStart, kw));
        }
        wrapper.orderByAsc(CampusHero::getSortOrder).orderByDesc(CampusHero::getUpdatedAt);
        Page<CampusHero> result = campusHeroMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<CampusHeroVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), safeSize, safePage);
    }

    @Override
    public CampusHeroVO getById(Long id) {
        CampusHero hero = campusHeroMapper.selectById(id);
        if (hero == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        return toVO(hero);
    }

    @Override
    public CampusHeroVO create(CampusHeroDTO dto) {
        String pageKey = normalizePageKey(dto.getPageKey());
        if (pageKey == null || pageKey.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "页面标识不能为空");
        }
        boolean exists = campusHeroMapper.selectCount(
            new LambdaQueryWrapper<CampusHero>().eq(CampusHero::getPageKey, pageKey)
        ) > 0;
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "页面标识已存在");
        }
        CampusHero hero = new CampusHero();
        apply(hero, dto);
        hero.setPageKey(pageKey);
        campusHeroMapper.insert(hero);
        return toVO(hero);
    }

    @Override
    public CampusHeroVO update(Long id, CampusHeroDTO dto) {
        CampusHero hero = campusHeroMapper.selectById(id);
        if (hero == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        String pageKey = normalizePageKey(dto.getPageKey());
        if (pageKey == null || pageKey.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "页面标识不能为空");
        }
        Long count = campusHeroMapper.selectCount(
            new LambdaQueryWrapper<CampusHero>()
                .eq(CampusHero::getPageKey, pageKey)
                .ne(CampusHero::getId, id)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "页面标识已存在");
        }
        apply(hero, dto);
        hero.setPageKey(pageKey);
        campusHeroMapper.updateById(hero);
        return toVO(hero);
    }

    @Override
    public void delete(Long id) {
        campusHeroMapper.deleteById(id);
    }

    @Override
    public CampusHeroVO getByPageKey(String pageKey) {
        String normalized = normalizePageKey(pageKey);
        if (normalized == null) {
            return null;
        }
        CampusHero hero = campusHeroMapper.selectOne(
            new LambdaQueryWrapper<CampusHero>()
                .eq(CampusHero::getPageKey, normalized)
                .eq(CampusHero::getEnabled, true)
        );
        if (hero == null) {
            return null;
        }
        CampusHeroVO vo = toVO(hero);
        applyRuntimeStats(hero, vo);
        return vo;
    }

    private void apply(CampusHero hero, CampusHeroDTO dto) {
        hero.setPageName(trimToNull(dto.getPageName()));
        hero.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : Boolean.TRUE);
        hero.setTheme(trimToNull(dto.getTheme()));
        hero.setTitleStart(trimToNull(dto.getTitleStart()));
        hero.setTitleHighlight(trimToNull(dto.getTitleHighlight()));
        hero.setDescription(trimToNull(dto.getDescription()));
        hero.setBadge(trimToNull(dto.getBadge()));
        hero.setPrimaryBtnText(trimToNull(dto.getPrimaryBtnText()));
        hero.setPrimaryBtnLink(trimToNull(dto.getPrimaryBtnLink()));
        hero.setSecondaryBtnText(trimToNull(dto.getSecondaryBtnText()));
        hero.setSecondaryBtnLink(trimToNull(dto.getSecondaryBtnLink()));
        hero.setShowStats(dto.getShowStats() != null ? dto.getShowStats() : Boolean.TRUE);
        hero.setStatsNumber(trimToNull(dto.getStatsNumber()));
        hero.setStatsLabel(trimToNull(dto.getStatsLabel()));
        hero.setAvatarUrls(dto.getAvatarUrls() != null ? dto.getAvatarUrls() : new ArrayList<>());
        hero.setFloatCardLabel(trimToNull(dto.getFloatCardLabel()));
        hero.setFloatCardValue(trimToNull(dto.getFloatCardValue()));
        hero.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
    }

    private CampusHeroVO toVO(CampusHero hero) {
        CampusHeroVO vo = new CampusHeroVO();
        vo.setId(hero.getId());
        vo.setPageKey(hero.getPageKey());
        vo.setPageName(hero.getPageName());
        vo.setEnabled(hero.getEnabled());
        vo.setTheme(hero.getTheme());
        vo.setTitleStart(hero.getTitleStart());
        vo.setTitleHighlight(hero.getTitleHighlight());
        vo.setDescription(hero.getDescription());
        vo.setBadge(hero.getBadge());
        vo.setPrimaryBtnText(hero.getPrimaryBtnText());
        vo.setPrimaryBtnLink(hero.getPrimaryBtnLink());
        vo.setSecondaryBtnText(hero.getSecondaryBtnText());
        vo.setSecondaryBtnLink(hero.getSecondaryBtnLink());
        vo.setShowStats(hero.getShowStats());
        vo.setStatsNumber(hero.getStatsNumber());
        vo.setStatsLabel(hero.getStatsLabel());
        vo.setAvatarUrls(hero.getAvatarUrls());
        vo.setFloatCardLabel(hero.getFloatCardLabel());
        vo.setFloatCardValue(hero.getFloatCardValue());
        vo.setSortOrder(hero.getSortOrder());
        vo.setCreatedAt(hero.getCreatedAt());
        vo.setUpdatedAt(hero.getUpdatedAt());
        return vo;
    }

    private void applyRuntimeStats(CampusHero hero, CampusHeroVO vo) {
        if (Boolean.FALSE.equals(hero.getShowStats())) {
            vo.setStatsNumber(null);
            vo.setStatsLabel(null);
            vo.setAvatarUrls(new ArrayList<>());
            vo.setAvatarNames(new ArrayList<>());
            return;
        }
        String pageKey = hero.getPageKey();
        long count = resolveStatsCount(pageKey);
        vo.setStatsNumber(formatCount(count));
        vo.setStatsLabel(resolveStatsLabel(pageKey));
        List<User> recentUsers = loadRecentUsers();
        vo.setAvatarUrls(recentUsers.stream().map(User::getAvatar).toList());
        vo.setAvatarNames(recentUsers.stream().map(this::resolveDisplayName).toList());
    }

    private long resolveStatsCount(String pageKey) {
        if (pageKey == null) {
            return 0;
        }
        return switch (pageKey) {
            case "HOME" -> userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                    .eq(User::getDeleted, 0)
            );
            case "CONFESSIONS" -> countPostsByBoard("confessions");
            case "TREEHOLE" -> countPostsByBoard("treehole");
            case "HELP" -> countPostsByBoard("help");
            case "MARKET" -> countPostsByBoard("market");
            case "LOST_FOUND" -> countPostsByBoard("lost-found");
            default -> postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getStatus, 0));
        };
    }

    private long countPostsByBoard(String board) {
        return postMapper.selectCount(
            new LambdaQueryWrapper<Post>()
                .eq(Post::getBoard, board)
                .eq(Post::getStatus, 0)
        );
    }

    private String resolveStatsLabel(String pageKey) {
        if (pageKey == null) {
            return "内容已发布";
        }
        return switch (pageKey) {
            case "HOME" -> "同学已加入";
            case "CONFESSIONS" -> "表白已发布";
            case "TREEHOLE" -> "心声已收录";
            case "HELP" -> "求助已发布";
            case "MARKET" -> "闲置已上架";
            case "LOST_FOUND" -> "失物已发布";
            default -> "内容已发布";
        };
    }

    private List<User> loadRecentUsers() {
        return userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .orderByDesc(User::getLoginDate)
                .orderByDesc(User::getCreatedAt)
                .last("LIMIT " + AVATAR_LIMIT)
        );
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "";
        }
        String nickname = trimToNull(user.getNickname());
        if (nickname != null) {
            return nickname;
        }
        String username = trimToNull(user.getUsername());
        return username != null ? username : "";
    }

    private String normalizePageKey(String pageKey) {
        if (pageKey == null) {
            return null;
        }
        return pageKey.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatCount(long count) {
        if (count <= 0) {
            return "0";
        }
        if (count >= 10000) {
            double value = count / 10000.0;
            String text = String.format("%.1f", value);
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text + "万+";
        }
        return String.format("%,d+", count);
    }
}
