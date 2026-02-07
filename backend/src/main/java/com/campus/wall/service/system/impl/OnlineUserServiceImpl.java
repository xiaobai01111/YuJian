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
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public PageResult<OnlineUserVO> queryOnlineUsers(OnlineUserQueryDTO query) {
        String keyword = query.getKeyword();
        String ipaddr = query.getIpaddr();
        String lowerKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        String ipFilter = StringUtils.hasText(ipaddr) ? ipaddr.trim() : null;

        int pageSize = Math.max(query.getSize(), 1);
        int offset = Math.max(query.getOffset(), 0);
        int targetCount = Math.max(1, offset + pageSize);
        Comparator<OnlineUserVO> orderComparator = Comparator
            .comparing(OnlineUserVO::getLastActiveTime, Comparator.nullsLast(Long::compareTo))
            .reversed();
        PriorityQueue<OnlineUserVO> topUsers = new PriorityQueue<>(targetCount, orderComparator.reversed());
        Set<String> seenTokens = new HashSet<>();
        Set<Long> seenUserIds = new HashSet<>();
        long[] total = {0L};

        scanTokenValues(token -> {
            String normalizedToken = normalizeTokenValue(token);
            if (!StringUtils.hasText(normalizedToken) || !seenTokens.add(normalizedToken)) {
                return;
            }
            OnlineUserVO vo = buildOnlineUser(normalizedToken);
            if (vo == null) {
                removeOnlineToken(normalizedToken);
                return;
            }
            if (vo.getUserId() != null) {
                seenUserIds.add(vo.getUserId());
            }
            if (!matchesFilter(vo, lowerKeyword, ipFilter)) {
                return;
            }
            total[0]++;
            offerTopUsers(topUsers, vo, targetCount, orderComparator);
        });

        OnlineUserVO currentUser = buildCurrentOnlineUser();
        if (currentUser != null) {
            boolean exists = seenTokens.contains(currentUser.getToken())
                || (currentUser.getUserId() != null && seenUserIds.contains(currentUser.getUserId()));
            if (!exists && matchesFilter(currentUser, lowerKeyword, ipFilter)) {
                total[0]++;
                offerTopUsers(topUsers, currentUser, targetCount, orderComparator);
            }
        }

        List<OnlineUserVO> sorted = new ArrayList<>(topUsers);
        sorted.sort(orderComparator);
        int start = Math.min(offset, sorted.size());
        int end = Math.min(start + pageSize, sorted.size());
        List<OnlineUserVO> pageRecords = sorted.subList(start, end);

        return PageResult.of(pageRecords, total[0], pageSize, query.getPage());
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

    private boolean matchesFilter(OnlineUserVO user, String lowerKeyword, String ipFilter) {
        if (user == null) {
            return false;
        }
        if (lowerKeyword != null && !lowerKeyword.isEmpty()) {
            if (!containsIgnoreCase(user.getUsername(), lowerKeyword)
                && !containsIgnoreCase(user.getNickname(), lowerKeyword)
                && !Objects.toString(user.getUserId(), "").contains(lowerKeyword)) {
                return false;
            }
        }
        if (StringUtils.hasText(ipFilter)) {
            return user.getIpaddr() != null && user.getIpaddr().contains(ipFilter);
        }
        return true;
    }

    private void offerTopUsers(PriorityQueue<OnlineUserVO> heap,
                               OnlineUserVO candidate,
                               int limit,
                               Comparator<OnlineUserVO> orderComparator) {
        if (limit <= 0 || candidate == null) {
            return;
        }
        if (heap.size() < limit) {
            heap.offer(candidate);
            return;
        }
        OnlineUserVO worst = heap.peek();
        if (worst != null && orderComparator.compare(candidate, worst) > 0) {
            heap.poll();
            heap.offer(candidate);
        }
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

    private void scanTokenValues(Consumer<String> consumer) {
        if (consumer == null) {
            return;
        }
        scanTokensFromRedis(consumer);
        scanTokensFromStp(consumer);
    }

    private void scanTokensFromRedis(Consumer<String> consumer) {
        try {
            Long size = redisTemplate.opsForSet().size(CacheConstants.ONLINE_TOKENS);
            if (size == null || size == 0) {
                return;
            }
            try (Cursor<String> cursor = redisTemplate.opsForSet().scan(
                CacheConstants.ONLINE_TOKENS, ScanOptions.scanOptions().count(500).build())) {
                while (cursor.hasNext()) {
                    consumer.accept(cursor.next());
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void scanTokensFromStp(Consumer<String> consumer) {
        scanTokenValuesBySearch(consumer, (start, size) -> StpUtil.searchTokenValue("", start, size, false));
        scanTokenValuesBySearch(consumer, (start, size) ->
            normalizeTokenSessionIds(StpUtil.searchTokenSessionId("", start, size, false)));

        int start = 0;
        int batchSize = 500;
        while (true) {
            List<String> sessionIds = StpUtil.searchSessionId("", start, batchSize, false);
            if (sessionIds == null || sessionIds.isEmpty()) {
                break;
            }
            List<String> loginIds = normalizeSessionIds(sessionIds);
            for (String loginId : loginIds) {
                if (!StringUtils.hasText(loginId)) {
                    continue;
                }
                List<String> tokens = StpUtil.getTokenValueListByLoginId(loginId);
                if (tokens == null || tokens.isEmpty()) {
                    continue;
                }
                for (String token : tokens) {
                    consumer.accept(token);
                }
            }
            if (sessionIds.size() < batchSize) {
                break;
            }
            start += batchSize;
        }
    }

    private void scanTokenValuesBySearch(Consumer<String> consumer,
                                         BiFunction<Integer, Integer, List<String>> fetcher) {
        if (fetcher == null) {
            return;
        }
        int start = 0;
        int batchSize = 500;
        while (true) {
            List<String> values = fetcher.apply(start, batchSize);
            if (values == null || values.isEmpty()) {
                break;
            }
            for (String value : values) {
                consumer.accept(value);
            }
            if (values.size() < batchSize) {
                break;
            }
            start += batchSize;
        }
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
