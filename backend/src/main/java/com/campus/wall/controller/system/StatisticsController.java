package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.common.R;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.user.User;
import com.campus.wall.entity.system.SysNotice;
import com.campus.wall.entity.system.Report;
import com.campus.wall.entity.user.IdentityVerification;
import com.campus.wall.mapper.system.SysNoticeMapper;
import com.campus.wall.mapper.system.ReportMapper;
import com.campus.wall.mapper.system.SensitiveWordMapper;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
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

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        boolean canUser = StpUtil.hasPermission("system:user:list");
        boolean canPost = StpUtil.hasPermission("content:post:list");
        boolean canNotice = StpUtil.hasPermission("system:notice:list");
        boolean canSensitive = StpUtil.hasPermission("system:sensitive-word:list");
        boolean canReport = StpUtil.hasPermission("content:report:list");
        boolean canVerify = StpUtil.hasPermission("content:verification:list");

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = todayStart;

        if (canUser) {
            Long totalUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)
            );
            stats.put("totalUsers", totalUsers);

            Long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                    .eq(User::getDeleted, 0)
                    .ge(User::getCreatedAt, todayStart)
            );
            stats.put("todayNewUsers", todayNewUsers);

            Long yesterdayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                    .eq(User::getDeleted, 0)
                    .ge(User::getCreatedAt, yesterdayStart)
                    .lt(User::getCreatedAt, yesterdayEnd)
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
                new LambdaQueryWrapper<Post>().eq(Post::getStatus, 0)
            );
            stats.put("totalPosts", totalPosts);

            Long todayPosts = postMapper.selectCount(
                new LambdaQueryWrapper<Post>()
                    .eq(Post::getStatus, 0)
                    .ge(Post::getCreatedAt, todayStart)
            );
            stats.put("todayPosts", todayPosts);

            Long yesterdayPosts = postMapper.selectCount(
                new LambdaQueryWrapper<Post>()
                    .eq(Post::getStatus, 0)
                    .ge(Post::getCreatedAt, yesterdayStart)
                    .lt(Post::getCreatedAt, yesterdayEnd)
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
                new LambdaQueryWrapper<IdentityVerification>().eq(IdentityVerification::getStatus, 0)
            );
            stats.put("pendingVerifications", pendingVerifications);
        }

        if (canReport) {
            Long pendingReports = reportMapper.selectCount(
                new LambdaQueryWrapper<Report>().eq(Report::getStatus, 0)
            );
            stats.put("pendingReports", pendingReports);
        }

        if (canNotice) {
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

        return R.ok(stats);
    }

    @Operation(summary = "获取最近活动")
    @GetMapping("/recent-activities")
    public R<List<Map<String, Object>>> getRecentActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();

        if (!StpUtil.hasPermission("content:post:list")) {
            return R.ok(activities);
        }

        // 获取最近的帖子
        List<Post> recentPosts = postMapper.selectList(
            new LambdaQueryWrapper<Post>()
                .orderByDesc(Post::getCreatedAt)
                .last("LIMIT 10")
        );

        for (Post post : recentPosts) {
            Map<String, Object> activity = new HashMap<>();
            
            // 获取用户信息
            User user = userMapper.selectById(post.getUserId());
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
        if (!StpUtil.hasPermission("system:notice:list")) {
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
