package com.campus.wall.service.system.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.PageResult;
import com.campus.wall.constant.CacheConstants;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.dto.system.OnlineUserQueryDTO;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.OnlineUserService;
import com.campus.wall.vo.system.OnlineUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public PageResult<OnlineUserVO> queryOnlineUsers(OnlineUserQueryDTO query) {
        List<String> tokenValues = resolveTokenValues();
        String keyword = query.getKeyword();
        String ipaddr = query.getIpaddr();

        List<OnlineUserVO> allUsers = new ArrayList<>();
        for (String token : tokenValues) {
            OnlineUserVO vo = buildOnlineUser(token);
            if (vo != null) {
                allUsers.add(vo);
            } else {
                removeOnlineToken(token);
            }
        }

        OnlineUserVO currentUser = buildCurrentOnlineUser();
        if (currentUser != null && allUsers.stream().noneMatch(item ->
            Objects.equals(item.getToken(), currentUser.getToken()) || Objects.equals(item.getUserId(), currentUser.getUserId()))) {
            allUsers.add(currentUser);
        }

        if (StringUtils.hasText(keyword)) {
            String lowerKeyword = keyword.trim().toLowerCase();
            allUsers = allUsers.stream()
                .filter(item -> containsIgnoreCase(item.getUsername(), lowerKeyword)
                    || containsIgnoreCase(item.getNickname(), lowerKeyword)
                    || Objects.toString(item.getUserId(), "").contains(lowerKeyword))
                .collect(Collectors.toList());
        }

        if (StringUtils.hasText(ipaddr)) {
            String ip = ipaddr.trim();
            allUsers = allUsers.stream()
                .filter(item -> item.getIpaddr() != null && item.getIpaddr().contains(ip))
                .collect(Collectors.toList());
        }

        allUsers.sort(Comparator.comparing(OnlineUserVO::getLastActiveTime, Comparator.nullsLast(Long::compareTo)).reversed());

        int total = allUsers.size();
        int start = Math.min(query.getOffset(), total);
        int end = Math.min(start + query.getSize(), total);
        List<OnlineUserVO> pageRecords = allUsers.subList(start, end);

        return PageResult.of(pageRecords, total, query.getSize(), query.getPage());
    }

    @Override
    public void kickoutByToken(String token) {
        StpUtil.kickoutByTokenValue(token);
        removeOnlineToken(token);
    }

    private OnlineUserVO buildOnlineUser(String token) {
        String normalizedToken = normalizeTokenValue(token);
        Object loginId = StpUtil.getLoginIdByToken(normalizedToken);
        if (loginId == null) {
            return null;
        }

        Long userId;
        try {
            userId = Long.valueOf(loginId.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }

        SaSession tokenSession = StpUtil.getStpLogic().getTokenSessionByToken(normalizedToken, false);
        String username = tokenSession != null ? (String) tokenSession.get(SecurityConstants.TOKEN_SESSION_USERNAME) : null;
        String nickname = tokenSession != null ? (String) tokenSession.get(SecurityConstants.TOKEN_SESSION_NICKNAME) : null;
        String ipaddr = tokenSession != null ? (String) tokenSession.get(SecurityConstants.TOKEN_SESSION_IP) : null;
        String userAgent = tokenSession != null ? (String) tokenSession.get(SecurityConstants.TOKEN_SESSION_USER_AGENT) : null;
        String loginTime = tokenSession != null ? (String) tokenSession.get(SecurityConstants.TOKEN_SESSION_LOGIN_TIME) : null;

        // 如果 token session 中缺少必要信息，从数据库获取用户
        boolean needFetchUser = !StringUtils.hasText(username) || !StringUtils.hasText(nickname) || !StringUtils.hasText(loginTime);
        User user;
        if (needFetchUser) {
            user = userMapper.selectById(userId);
            if (user == null) {
                return null;
            }
            if (!StringUtils.hasText(username)) {
                username = user.getUsername();
            }
            if (!StringUtils.hasText(nickname)) {
                nickname = user.getNickname();
            }
            if (!StringUtils.hasText(loginTime) && user.getLoginDate() != null) {
                loginTime = user.getLoginDate().toString();
            }
        }

        OnlineUserVO vo = new OnlineUserVO();
        vo.setToken(normalizedToken);
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setNickname(nickname);
        vo.setIpaddr(ipaddr);
        vo.setUserAgent(userAgent);
        vo.setLoginTime(loginTime);
        Long lastActiveTime = StpUtil.getStpLogic().getTokenLastActiveTime(normalizedToken);
        vo.setLastActiveTime(resolveLastActiveTime(lastActiveTime, loginTime));
        vo.setTokenTimeout(StpUtil.getTokenTimeout(normalizedToken));
        vo.setTokenActiveTimeout(StpUtil.getStpLogic().getTokenActiveTimeoutByToken(normalizedToken));
        return vo;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return value.toLowerCase().contains(keyword);
    }

    private List<String> normalizeTokenValues(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return new ArrayList<>();
        }
        return rawValues.stream()
            .map(this::normalizeTokenValue)
            .collect(Collectors.toList());
    }

    private List<String> normalizeSessionIds(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return new ArrayList<>();
        }
        String prefix = StpUtil.getStpLogic().splicingKeySession("");
        return rawValues.stream()
            .map(value -> value != null && value.startsWith(prefix) ? value.substring(prefix.length()) : value)
            .collect(Collectors.toList());
    }

    private List<String> normalizeTokenSessionIds(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return new ArrayList<>();
        }
        String prefix = StpUtil.getStpLogic().splicingKeyTokenSession("");
        return rawValues.stream()
            .map(value -> value != null && value.startsWith(prefix) ? value.substring(prefix.length()) : value)
            .collect(Collectors.toList());
    }

    private List<String> resolveTokenValues() {
        List<String> tokenValues = normalizeTokenValues(
            StpUtil.searchTokenValue("", 0, Integer.MAX_VALUE, false)
        );
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>(tokenValues);

        try {
            var cachedTokens = redisTemplate.opsForSet().members(CacheConstants.ONLINE_TOKENS);
            if (cachedTokens != null) {
                for (String token : cachedTokens) {
                    merged.add(normalizeTokenValue(token));
                }
            }
        } catch (Exception ignored) {
        }

        List<String> tokenSessionIds = normalizeTokenSessionIds(
            StpUtil.searchTokenSessionId("", 0, Integer.MAX_VALUE, false)
        );
        for (String token : tokenSessionIds) {
            if (StringUtils.hasText(token)) {
                merged.add(normalizeTokenValue(token));
            }
        }

        List<String> sessionIds = normalizeSessionIds(
            StpUtil.searchSessionId("", 0, Integer.MAX_VALUE, false)
        );
        for (String loginId : sessionIds) {
            if (!StringUtils.hasText(loginId)) {
                continue;
            }
            merged.addAll(StpUtil.getTokenValueListByLoginId(loginId));
        }

        String currentToken = normalizeTokenValue(getCurrentTokenSafe());
        if (StringUtils.hasText(currentToken)) {
            merged.add(currentToken);
        }

        return new ArrayList<>(merged);
    }

    private String getCurrentTokenSafe() {
        try {
            return StpUtil.getTokenValue();
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeTokenValue(String tokenValue) {
        if (!StringUtils.hasText(tokenValue)) {
            return null;
        }
        String trimmed = tokenValue.trim();
        String prefix = StpUtil.getStpLogic().splicingKeyTokenValue("");
        if (trimmed.startsWith(prefix)) {
            trimmed = trimmed.substring(prefix.length());
        }
        int tokenIndex = trimmed.lastIndexOf(":token:");
        if (tokenIndex >= 0) {
            trimmed = trimmed.substring(tokenIndex + 7);
        }
        return trimmed;
    }

    private void removeOnlineToken(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            redisTemplate.opsForSet().remove(CacheConstants.ONLINE_TOKENS, normalizeTokenValue(token));
        } catch (Exception ignored) {
        }
    }

    private OnlineUserVO buildCurrentOnlineUser() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId == null) {
                return null;
            }
            Long userId = Long.valueOf(loginId.toString());
            User user = userMapper.selectById(userId);
            if (user == null) {
                return null;
            }
            String token = normalizeTokenValue(getCurrentTokenSafe());
            SaSession tokenSession = StpUtil.getTokenSession();
            String username = tokenSession != null ? (String) tokenSession.get(SecurityConstants.TOKEN_SESSION_USERNAME) : null;
            String nickname = tokenSession != null ? (String) tokenSession.get(SecurityConstants.TOKEN_SESSION_NICKNAME) : null;
            String ipaddr = tokenSession != null ? (String) tokenSession.get(SecurityConstants.TOKEN_SESSION_IP) : null;
            String userAgent = tokenSession != null ? (String) tokenSession.get(SecurityConstants.TOKEN_SESSION_USER_AGENT) : null;
            String loginTime = tokenSession != null ? (String) tokenSession.get(SecurityConstants.TOKEN_SESSION_LOGIN_TIME) : null;

            if (!StringUtils.hasText(username)) {
                username = user.getUsername();
            }
            if (!StringUtils.hasText(nickname)) {
                nickname = user.getNickname();
            }
            if (!StringUtils.hasText(loginTime) && user.getLoginDate() != null) {
                loginTime = user.getLoginDate().toString();
            }

            OnlineUserVO vo = new OnlineUserVO();
            vo.setToken(token);
            vo.setUserId(userId);
            vo.setUsername(username);
            vo.setNickname(nickname);
            vo.setIpaddr(ipaddr);
            vo.setUserAgent(userAgent);
            vo.setLoginTime(loginTime);
            if (StringUtils.hasText(token)) {
                Long lastActiveTime = StpUtil.getStpLogic().getTokenLastActiveTime(token);
                vo.setLastActiveTime(resolveLastActiveTime(lastActiveTime, loginTime));
                vo.setTokenTimeout(StpUtil.getTokenTimeout(token));
                vo.setTokenActiveTimeout(StpUtil.getStpLogic().getTokenActiveTimeoutByToken(token));
            }
            return vo;
        } catch (Exception e) {
            return null;
        }
    }

    private Long resolveLastActiveTime(Long lastActiveTime, String loginTime) {
        if (lastActiveTime != null && lastActiveTime > 0) {
            return lastActiveTime;
        }
        Long fallback = parseLoginTime(loginTime);
        return fallback != null && fallback > 0 ? fallback : null;
    }

    private Long parseLoginTime(String loginTime) {
        if (!StringUtils.hasText(loginTime)) {
            return null;
        }
        String trimmed = loginTime.trim();
        LocalDateTime parsed = parseLocalDateTime(trimmed);
        if (parsed == null) {
            return null;
        }
        return parsed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private LocalDateTime parseLocalDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
        }
        return null;
    }
}
