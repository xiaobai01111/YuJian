package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.common.R;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.user.User;
import com.campus.wall.entity.system.SysLoginLog;
import com.campus.wall.entity.system.SysNotice;
import com.campus.wall.entity.system.Report;
import com.campus.wall.entity.system.SysOperLog;
import com.campus.wall.entity.user.IdentityVerification;
import com.campus.wall.mapper.system.SysNoticeMapper;
import com.campus.wall.mapper.system.ReportMapper;
import com.campus.wall.mapper.system.SensitiveWordMapper;
import com.campus.wall.mapper.system.SysLoginLogMapper;
import com.campus.wall.mapper.system.SysOperLogMapper;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.security.DataScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计数据控制器
 */
@Tag(name = "统计数据", description = "仪表盘统计数据接口")
@RestController
@RequestMapping("/api/v1/console/statistics")
@RequiredArgsConstructor
@SaCheckLogin
public class StatisticsController {

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final SysNoticeMapper noticeMapper;
    private final SensitiveWordMapper sensitiveWordMapper;
    private final ReportMapper reportMapper;
    private final IdentityVerificationMapper identityVerificationMapper;
    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final DataScopeService dataScopeService;

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        boolean canUser = StpUtil.hasPermission("system:dashboard:user");
        boolean canPost = StpUtil.hasPermission("system:dashboard:post");
        boolean canNoticeOverview = StpUtil.hasPermission("system:dashboard:notice:overview");
        boolean canSensitive = StpUtil.hasPermission("system:dashboard:ops");
        boolean canReport = StpUtil.hasPermission("system:dashboard:report");
        boolean canVerify = StpUtil.hasPermission("system:dashboard:verify");
        boolean canLoginLog = StpUtil.hasPermission("system:dashboard:login");

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = todayStart;

        Long userId = StpUtil.getLoginIdAsLong();
        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        boolean allowAll = scope.isAllowAll();
        List<Long> allowedUserIds = allowAll ? List.of() : dataScopeService.resolveAllowedUserIds(scope, userId);
        List<Long> allowedPostIds = allowAll ? List.of() : resolveScopedPostIds(allowedUserIds);

        if (canUser) {
            Long totalUsers = userMapper.selectCount(
                applyUserScope(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0), allowAll, allowedUserIds)
            );
            stats.put("totalUsers", totalUsers);

            Long todayNewUsers = userMapper.selectCount(
                applyUserScope(new LambdaQueryWrapper<User>()
                    .eq(User::getDeleted, 0)
                    .ge(User::getCreatedAt, todayStart), allowAll, allowedUserIds)
            );
            stats.put("todayNewUsers", todayNewUsers);

            Long yesterdayNewUsers = userMapper.selectCount(
                applyUserScope(new LambdaQueryWrapper<User>()
                    .eq(User::getDeleted, 0)
                    .ge(User::getCreatedAt, yesterdayStart)
                    .lt(User::getCreatedAt, yesterdayEnd), allowAll, allowedUserIds)
            );

            double userGrowth = 0;
            if (yesterdayNewUsers > 0) {
                userGrowth = ((double) (todayNewUsers - yesterdayNewUsers) / yesterdayNewUsers) * 100;
            } else if (todayNewUsers > 0) {
                userGrowth = 100;
            }
            stats.put("userGrowth", Math.round(userGrowth));
        }

        if (canPost) {
            Long totalPosts = postMapper.selectCount(
                applyPostScope(new LambdaQueryWrapper<Post>().eq(Post::getStatus, 0), allowAll, allowedUserIds)
            );
            stats.put("totalPosts", totalPosts);

            Long todayPosts = postMapper.selectCount(
                applyPostScope(new LambdaQueryWrapper<Post>()
                    .eq(Post::getStatus, 0)
                    .ge(Post::getCreatedAt, todayStart), allowAll, allowedUserIds)
            );
            stats.put("todayPosts", todayPosts);

            Long yesterdayPosts = postMapper.selectCount(
                applyPostScope(new LambdaQueryWrapper<Post>()
                    .eq(Post::getStatus, 0)
                    .ge(Post::getCreatedAt, yesterdayStart)
                    .lt(Post::getCreatedAt, yesterdayEnd), allowAll, allowedUserIds)
            );
            stats.put("yesterdayPosts", yesterdayPosts);

            double postGrowth = 0;
            if (yesterdayPosts > 0) {
                postGrowth = ((double) (todayPosts - yesterdayPosts) / yesterdayPosts) * 100;
            } else if (todayPosts > 0) {
                postGrowth = 100;
            }
            stats.put("postGrowth", Math.round(postGrowth));
        }

        if (canVerify) {
            Long pendingVerifications = identityVerificationMapper.selectCount(
                applyVerificationScope(new LambdaQueryWrapper<IdentityVerification>()
                    .eq(IdentityVerification::getStatus, 0), allowAll, allowedUserIds)
            );
            stats.put("pendingVerifications", pendingVerifications);
        }

        if (canReport) {
            Long pendingReports = reportMapper.selectCount(
                applyReportScope(new LambdaQueryWrapper<Report>()
                    .eq(Report::getStatus, 0)
                    .eq(Report::getDeleted, 0), allowAll, allowedPostIds)
            );
            stats.put("pendingReports", pendingReports);
        }

        if (canNoticeOverview) {
            Long noticeTotal = noticeMapper.selectCount(new LambdaQueryWrapper<>());
            stats.put("noticeTotal", noticeTotal);

            Long noticePublished = noticeMapper.selectCount(
                new LambdaQueryWrapper<SysNotice>().eq(SysNotice::getStatus, 1)
            );
            stats.put("noticePublished", noticePublished);

            Long noticeDraft = noticeMapper.selectCount(
                new LambdaQueryWrapper<SysNotice>().eq(SysNotice::getStatus, 0)
            );
            stats.put("noticeDraft", noticeDraft);

            Long noticeOffline = noticeMapper.selectCount(
                new LambdaQueryWrapper<SysNotice>().eq(SysNotice::getStatus, 2)
            );
            stats.put("noticeOffline", noticeOffline);

            Long noticePinned = noticeMapper.selectCount(
                new LambdaQueryWrapper<SysNotice>()
                    .eq(SysNotice::getStatus, 1)
                    .eq(SysNotice::getIsPinned, true)
            );
            stats.put("noticePinned", noticePinned);

            LocalDateTime soon = todayStart.plusDays(7);
            Long noticeExpiringSoon = noticeMapper.selectCount(
                new LambdaQueryWrapper<SysNotice>()
                    .eq(SysNotice::getStatus, 1)
                    .isNotNull(SysNotice::getEndAt)
                    .ge(SysNotice::getEndAt, todayStart)
                    .lt(SysNotice::getEndAt, soon)
            );
            stats.put("noticeExpiringSoon", noticeExpiringSoon);
        }

        if (canSensitive) {
            Long sensitiveWords = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<>());
            stats.put("sensitiveWords", sensitiveWords);
        }

        if (canLoginLog) {
            Long loginSuccessToday = loginLogMapper.selectCount(
                new LambdaQueryWrapper<SysLoginLog>()
                    .ge(SysLoginLog::getLoginTime, todayStart)
                    .eq(SysLoginLog::getStatus, 0)
            );
            Long loginFailToday = loginLogMapper.selectCount(
                new LambdaQueryWrapper<SysLoginLog>()
                    .ge(SysLoginLog::getLoginTime, todayStart)
                    .eq(SysLoginLog::getStatus, 1)
            );
            stats.put("loginSuccessToday", loginSuccessToday);
            stats.put("loginFailToday", loginFailToday);
            stats.put("loginTotalToday", loginSuccessToday + loginFailToday);
        }

        return R.ok(stats);
    }

    @Operation(summary = "获取最近活动")
    @GetMapping("/recent-activities")
    public R<List<Map<String, Object>>> getRecentActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();

        if (!StpUtil.hasPermission("system:dashboard:post")) {
            return R.ok(activities);
        }

        Long userId = StpUtil.getLoginIdAsLong();
        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        boolean allowAll = scope.isAllowAll();
        List<Long> allowedUserIds = allowAll ? List.of() : dataScopeService.resolveAllowedUserIds(scope, userId);

        // 获取最近的帖子
        List<Post> recentPosts = postMapper.selectList(
            applyPostScope(new LambdaQueryWrapper<Post>()
                .orderByDesc(Post::getCreatedAt)
                .last("LIMIT 10"), allowAll, allowedUserIds)
        );

        Map<Long, User> userMap = recentPosts.isEmpty() ? Map.of() : userMapper.selectBatchIds(
                recentPosts.stream()
                        .map(Post::getUserId)
                        .filter(id -> id != null)
                        .distinct()
                        .collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        for (Post post : recentPosts) {
            Map<String, Object> activity = new HashMap<>();
            
            // 获取用户信息
            User user = userMap.get(post.getUserId());
            String nickname = user != null ? user.getNickname() : "未知用户";
            String avatar = user != null ? user.getAvatar() : null;
            
            activity.put("userId", post.getUserId());
            activity.put("nickname", nickname);
            activity.put("avatar", avatar);
            activity.put("action", getActionText(post));
            activity.put("time", post.getCreatedAt());
            activity.put("status", post.getStatus());
            activity.put("postId", post.getId());
            
            activities.add(activity);
        }

        return R.ok(activities);
    }

    @Operation(summary = "获取最新公告")
    @GetMapping("/recent-notices")
    public R<List<Map<String, Object>>> getRecentNotices() {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!StpUtil.hasPermission("system:dashboard:notice:list")) {
            return R.ok(result);
        }

        List<SysNotice> notices = noticeMapper.selectList(
            new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, 1)
                .orderByDesc(SysNotice::getIsPinned)
                .orderByDesc(SysNotice::getPublishedAt)
                .last("LIMIT 6")
        );

        for (SysNotice notice : notices) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", notice.getId());
            item.put("title", notice.getTitle());
            item.put("isPinned", notice.getIsPinned());
            item.put("publishedAt", notice.getPublishedAt());
            item.put("status", notice.getStatus());
            result.add(item);
        }

        return R.ok(result);
    }

    @Operation(summary = "获取最新举报")
    @GetMapping("/recent-reports")
    public R<List<Map<String, Object>>> getRecentReports() {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!StpUtil.hasPermission("system:dashboard:report")) {
            return R.ok(result);
        }

        Long userId = StpUtil.getLoginIdAsLong();
        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        boolean allowAll = scope.isAllowAll();
        List<Long> allowedUserIds = allowAll ? List.of() : dataScopeService.resolveAllowedUserIds(scope, userId);
        List<Long> allowedPostIds = allowAll ? List.of() : resolveScopedPostIds(allowedUserIds);

        List<Report> reports = reportMapper.selectList(
            applyReportScope(new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, 0)
                .eq(Report::getDeleted, 0)
                .orderByDesc(Report::getCreatedAt)
                .last("LIMIT 6"), allowAll, allowedPostIds)
        );

        Map<Long, User> reporterMap = reports.isEmpty() ? Map.of() : userMapper.selectBatchIds(
                reports.stream()
                        .map(Report::getReporterId)
                        .filter(id -> id != null)
                        .distinct()
                        .collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        for (Report report : reports) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", report.getId());
            item.put("reason", report.getReason());
            item.put("status", report.getStatus());
            item.put("createdAt", report.getCreatedAt());
            User reporter = reporterMap.get(report.getReporterId());
            item.put("reporterName", reporter != null ? reporter.getNickname() : "未知用户");
            item.put("postId", report.getPostId());
            result.add(item);
        }

        return R.ok(result);
    }

    @Operation(summary = "获取最新审核")
    @GetMapping("/recent-verifications")
    public R<List<Map<String, Object>>> getRecentVerifications() {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!StpUtil.hasPermission("system:dashboard:verify")) {
            return R.ok(result);
        }

        Long userId = StpUtil.getLoginIdAsLong();
        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        boolean allowAll = scope.isAllowAll();
        List<Long> allowedUserIds = allowAll ? List.of() : dataScopeService.resolveAllowedUserIds(scope, userId);

        List<IdentityVerification> verifications = identityVerificationMapper.selectList(
            applyVerificationScope(new LambdaQueryWrapper<IdentityVerification>()
                .eq(IdentityVerification::getStatus, 0)
                .orderByDesc(IdentityVerification::getCreatedAt)
                .last("LIMIT 6"), allowAll, allowedUserIds)
        );

        Map<Long, User> verifyUserMap = verifications.isEmpty() ? Map.of() : userMapper.selectBatchIds(
                verifications.stream()
                        .map(IdentityVerification::getUserId)
                        .filter(id -> id != null)
                        .distinct()
                        .collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        for (IdentityVerification verification : verifications) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", verification.getId());
            item.put("status", verification.getStatus());
            item.put("createdAt", verification.getCreatedAt());
            item.put("userId", verification.getUserId());
            User user = verifyUserMap.get(verification.getUserId());
            item.put("nickname", user != null ? user.getNickname() : "未知用户");
            result.add(item);
        }

        return R.ok(result);
    }

    @Operation(summary = "获取最近操作日志")
    @GetMapping("/recent-oper-logs")
    public R<List<Map<String, Object>>> getRecentOperLogs() {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!StpUtil.hasPermission("system:dashboard:operlog")) {
            return R.ok(result);
        }

        List<SysOperLog> logs = operLogMapper.selectList(
            new LambdaQueryWrapper<SysOperLog>()
                .orderByDesc(SysOperLog::getCreatedAt)
                .last("LIMIT 6")
        );

        for (SysOperLog log : logs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", log.getId());
            item.put("operatorName", log.getOperatorName());
            item.put("action", log.getAction());
            item.put("targetType", log.getTargetType());
            item.put("ipAddress", log.getIpAddress());
            item.put("createdAt", log.getCreatedAt());
            result.add(item);
        }

        return R.ok(result);
    }

    @Operation(summary = "登录日志趋势")
    @GetMapping("/login-log-trend")
    public R<List<Map<String, Object>>> getLoginLogTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        if (!StpUtil.hasPermission("system:dashboard:login")) {
            return R.ok(trend);
        }

        LocalDate start = LocalDate.now().minusDays(6);
        LocalDate end = LocalDate.now();

        QueryWrapper<SysLoginLog> wrapper = new QueryWrapper<>();
        wrapper.select(
                "date(login_time) as day",
                "sum(case when status = 0 then 1 else 0 end) as success",
                "sum(case when status = 1 then 1 else 0 end) as fail"
            )
            .ge("login_time", start.atStartOfDay())
            .le("login_time", end.plusDays(1).atStartOfDay().minusNanos(1))
            .groupBy("date(login_time)")
            .orderByAsc("day");

        List<Map<String, Object>> rows = loginLogMapper.selectMaps(wrapper);
        Map<String, long[]> lookup = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String day = String.valueOf(row.get("day"));
            long success = toLong(row.get("success"));
            long fail = toLong(row.get("fail"));
            lookup.put(day, new long[] { success, fail });
        }

        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String day = cursor.toString();
            long[] counts = lookup.getOrDefault(day, new long[] { 0L, 0L });
            Map<String, Object> item = new HashMap<>();
            item.put("date", day);
            item.put("success", counts[0]);
            item.put("fail", counts[1]);
            trend.add(item);
            cursor = cursor.plusDays(1);
        }

        return R.ok(trend);
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return value != null ? Long.parseLong(value.toString()) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private LambdaQueryWrapper<User> applyUserScope(LambdaQueryWrapper<User> wrapper, boolean allowAll, List<Long> allowedUserIds) {
        if (allowAll) {
            return wrapper;
        }
        if (allowedUserIds == null || allowedUserIds.isEmpty()) {
            return wrapper.eq(User::getId, -1L);
        }
        return wrapper.in(User::getId, allowedUserIds);
    }

    private LambdaQueryWrapper<Post> applyPostScope(LambdaQueryWrapper<Post> wrapper, boolean allowAll, List<Long> allowedUserIds) {
        if (allowAll) {
            return wrapper;
        }
        if (allowedUserIds == null || allowedUserIds.isEmpty()) {
            return wrapper.eq(Post::getId, -1L);
        }
        return wrapper.in(Post::getUserId, allowedUserIds);
    }

    private LambdaQueryWrapper<Report> applyReportScope(LambdaQueryWrapper<Report> wrapper, boolean allowAll, List<Long> allowedPostIds) {
        if (allowAll) {
            return wrapper;
        }
        if (allowedPostIds == null || allowedPostIds.isEmpty()) {
            return wrapper.eq(Report::getId, -1L);
        }
        return wrapper.in(Report::getPostId, allowedPostIds);
    }

    private LambdaQueryWrapper<IdentityVerification> applyVerificationScope(LambdaQueryWrapper<IdentityVerification> wrapper, boolean allowAll, List<Long> allowedUserIds) {
        if (allowAll) {
            return wrapper;
        }
        if (allowedUserIds == null || allowedUserIds.isEmpty()) {
            return wrapper.eq(IdentityVerification::getId, -1L);
        }
        return wrapper.in(IdentityVerification::getUserId, allowedUserIds);
    }

    private List<Long> resolveScopedPostIds(List<Long> allowedUserIds) {
        if (allowedUserIds == null || allowedUserIds.isEmpty()) {
            return List.of();
        }
        return postMapper.selectList(
                        new LambdaQueryWrapper<Post>()
                                .select(Post::getId)
                                .in(Post::getUserId, allowedUserIds)
                ).stream()
                .map(Post::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String getActionText(Post post) {
        String board = post.getBoard();
        return switch (board) {
            case "confession" -> "发布了表白";
            case "treehole" -> "倾诉了心声";
            case "help" -> "发起了求助";
            case "market" -> "发布了闲置";
            case "lost" -> "发布了寻物";
            case "found" -> "发布了招领";
            default -> "发布了帖子";
        };
    }
}
