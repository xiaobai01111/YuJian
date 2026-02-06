package com.campus.wall.service.post.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.util.BoardUtil;
import com.campus.wall.util.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.post.PostCreateDTO;
import com.campus.wall.dto.post.PostQueryDTO;
import com.campus.wall.dto.post.PostUpdateDTO;
import com.campus.wall.entity.post.Bookmark;
import com.campus.wall.entity.post.Like;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.post.PostBoard;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.BookmarkMapper;
import com.campus.wall.mapper.post.LikeMapper;
import com.campus.wall.mapper.post.PostBoardMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.content.SensitiveWordService;
import com.campus.wall.service.file.FileService;
import com.campus.wall.service.post.PostService;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.vo.common.BatchActionResultVO;
import com.campus.wall.vo.file.FileVO;
import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.user.UserVO;
import com.campus.wall.constant.RateLimitConstants;
import com.campus.wall.service.security.RateLimitService;
import com.campus.wall.service.system.OperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 帖子服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final LikeMapper likeMapper;
    private final BookmarkMapper bookmarkMapper;
    private final PostBoardMapper postBoardMapper;
    private final UserMapper userMapper;
    private final FileService fileService;
    private final SensitiveWordService sensitiveWordService;
    private final com.campus.wall.service.user.CreditService creditService;
    private final RateLimitService rateLimitService;
    private final DataScopeService dataScopeService;
    private final OperLogService operLogService;
    private final StringRedisTemplate redisTemplate;

    // 帖子状态：0正常 1已解决 2已删除 3待审核 4已下架 5已售出
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_RESOLVED = 1;
    private static final int STATUS_DELETED = 2;
    @SuppressWarnings("unused")
    private static final int STATUS_PENDING_AUDIT = 3;
    @SuppressWarnings("unused")
    private static final int STATUS_ARCHIVED = 4;
    private static final int STATUS_SOLD = 5;
    // 前端可见的状态列表
    private static final List<Integer> VISIBLE_STATUSES = List.of(STATUS_NORMAL, STATUS_RESOLVED, STATUS_SOLD);
    private static final String VIEW_KEY_PREFIX = "post:view:";

    // 板块常量
    private static final String BOARD_TREE_HOLE = BoardUtil.BOARD_TREE_HOLE;
    private static final String BOARD_MARKET = BoardUtil.BOARD_MARKET;

    private static class PostUserContext {
        private final Map<Long, User> userMap;
        private final Set<Long> likedPostIds;
        private final Set<Long> bookmarkedPostIds;

        private PostUserContext(Map<Long, User> userMap, Set<Long> likedPostIds, Set<Long> bookmarkedPostIds) {
            this.userMap = userMap;
            this.likedPostIds = likedPostIds;
            this.bookmarkedPostIds = bookmarkedPostIds;
        }
    }

    @Override
    @Transactional
    public Long createPost(PostCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        rateLimitService.checkRateLimit(
            "rate:post:user:" + userId,
            RateLimitConstants.POST_LIMIT_PER_MINUTE,
            RateLimitConstants.WINDOW_SECONDS,
            ResultCode.RATE_LIMIT_EXCEEDED
        );

        // 用户验证状态检查
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        
        // 所有板块都需要验证用户身份才能发帖
        if (currentUser.getVerifyStatus() == null || currentUser.getVerifyStatus() != 2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "请先完成身份验证后再发帖");
        }
        
        // 规范化板块列表（兼容旧字段 board）
        List<String> boards = BoardUtil.normalizeBoardKeys(dto.getBoards());
        if (boards.isEmpty()) {
            String singleBoard = BoardUtil.normalizeBoardKey(dto.getBoard());
            if (singleBoard != null) {
                boards = List.of(singleBoard);
            }
        }
        if (boards.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "板块不能为空或无效");
        }

        // 市集板块额外需要信用分达标
        if (boards.contains(BOARD_MARKET)) {
            if (!creditService.canPostInMarket(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "信用分不足，无法在市集发帖（需要60分以上）");
            }
        }

        // 敏感词检测
        if (sensitiveWordService.containsSensitiveWord(dto.getTitle())) {
            throw new BusinessException(ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD, "标题包含敏感词");
        }
        if (sensitiveWordService.containsSensitiveWord(dto.getContent())) {
            throw new BusinessException(ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD, "内容包含敏感词");
        }

        // 树洞板块强制匿名
        Boolean isAnonymous = dto.getIsAnonymous();
        if (boards.contains(BOARD_TREE_HOLE)) {
            isAnonymous = true;
        }

        // 创建帖子
        Post post = new Post();
        post.setUserId(userId);
        post.setBoard(boards.get(0));
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setIsAnonymous(isAnonymous);
        post.setCategory(dto.getCategory());
        post.setPrice(dto.getPrice());
        post.setLocation(dto.getLocation());
        post.setLostTime(dto.getLostTime());
        Boolean showOnHome = dto.getShowOnHome();
        post.setShowOnHome(showOnHome == null ? Boolean.TRUE : showOnHome);
        post.setStatus(STATUS_NORMAL);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setLastInteractionAt(LocalDateTime.now());

        postMapper.insert(post);

        // 绑定板块
        savePostBoards(post.getId(), boards);

        // 绑定文件
        if (dto.getFileIds() != null && !dto.getFileIds().isEmpty()) {
            fileService.bindFiles(dto.getFileIds(), post.getId(), "post");
        }

        log.info("用户 {} 创建帖子: {}", userId, post.getId());
        return post.getId();
    }

    @Override
    @Transactional
    public Long createPostByAdmin(PostCreateDTO dto) {
        return createPost(dto);
    }

    @Override
    @Transactional
    public void updatePost(Long postId, PostUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Post post = getPostOrThrow(postId);

        // 权限校验：仅作者可编辑
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权编辑此帖子");
        }

        // 敏感词检测
        if (dto.getTitle() != null && sensitiveWordService.containsSensitiveWord(dto.getTitle())) {
            throw new BusinessException(ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD, "标题包含敏感词");
        }
        if (dto.getContent() != null && sensitiveWordService.containsSensitiveWord(dto.getContent())) {
            throw new BusinessException(ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD, "内容包含敏感词");
        }

        // 更新字段
        if (dto.getTitle() != null) {
            post.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            post.setContent(dto.getContent());
        }
        if (dto.getCategory() != null) {
            post.setCategory(dto.getCategory());
        }
        if (dto.getPrice() != null) {
            post.setPrice(dto.getPrice());
        }
        if (dto.getLocation() != null) {
            post.setLocation(dto.getLocation());
        }
        if (dto.getLostTime() != null) {
            post.setLostTime(dto.getLostTime());
        }
        if (dto.getShowOnHome() != null) {
            post.setShowOnHome(dto.getShowOnHome());
        }

        // 更新板块（可选）
        if (dto.getBoards() != null) {
            List<String> boards = BoardUtil.normalizeBoardKeys(dto.getBoards());
            if (boards.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "板块不能为空或无效");
            }
            if (boards.contains(BOARD_MARKET) && !creditService.canPostInMarket(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "信用分不足，无法在市集发帖（需要60分以上）");
            }
            if (boards.contains(BOARD_TREE_HOLE)) {
                post.setIsAnonymous(true);
            }
            post.setBoard(boards.get(0));
            replacePostBoards(postId, boards);
        }

        postMapper.updateById(post);

        // 处理文件变更
        if (dto.getAddFileIds() != null && !dto.getAddFileIds().isEmpty()) {
            fileService.bindFiles(dto.getAddFileIds(), postId, "post");
        }

        log.info("用户 {} 更新帖子: {}", userId, postId);
    }

    @Override
    @Transactional
    public void updatePostByAdmin(Long postId, PostUpdateDTO dto, String reason) {
        Long userId = StpUtil.getLoginIdAsLong();
        Post post = getPostOrThrow(postId);
        assertCanManagePost(post, reason, "update");

        // 敏感词检测
        if (dto.getTitle() != null && sensitiveWordService.containsSensitiveWord(dto.getTitle())) {
            throw new BusinessException(ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD, "标题包含敏感词");
        }
        if (dto.getContent() != null && sensitiveWordService.containsSensitiveWord(dto.getContent())) {
            throw new BusinessException(ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD, "内容包含敏感词");
        }

        // 更新字段
        if (dto.getTitle() != null) {
            post.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            post.setContent(dto.getContent());
        }
        if (dto.getCategory() != null) {
            post.setCategory(dto.getCategory());
        }
        if (dto.getPrice() != null) {
            post.setPrice(dto.getPrice());
        }
        if (dto.getLocation() != null) {
            post.setLocation(dto.getLocation());
        }
        if (dto.getLostTime() != null) {
            post.setLostTime(dto.getLostTime());
        }
        if (dto.getShowOnHome() != null) {
            post.setShowOnHome(dto.getShowOnHome());
        }

        // 更新板块（可选）
        if (dto.getBoards() != null) {
            List<String> boards = BoardUtil.normalizeBoardKeys(dto.getBoards());
            if (boards.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "板块不能为空或无效");
            }
            if (boards.contains(BOARD_MARKET) && !creditService.canPostInMarket(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "信用分不足，无法在市集发帖（需要60分以上）");
            }
            if (boards.contains(BOARD_TREE_HOLE)) {
                post.setIsAnonymous(true);
            }
            post.setBoard(boards.get(0));
            replacePostBoards(postId, boards);
        }

        postMapper.updateById(post);

        // 处理文件变更
        if (dto.getAddFileIds() != null && !dto.getAddFileIds().isEmpty()) {
            fileService.bindFiles(dto.getAddFileIds(), postId, "post");
        }

        log.info("管理员 {} 更新帖子: {}", userId, postId);
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Post post = getPostOrThrow(postId);

        // 权限校验：作者或管理员可删除
        boolean isAdmin = StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey());
        if (!post.getUserId().equals(userId) && !isAdmin) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此帖子");
        }

        // 软删除
        post.setStatus(STATUS_DELETED);
        postMapper.updateById(post);

        log.info("用户 {} 删除帖子: {}", userId, postId);
    }

    @Override
    @Transactional
    public void deletePostByAdmin(Long postId, String reason) {
        Post post = getPostOrThrow(postId);
        assertCanManagePost(post, reason, "delete");
        if (post.getStatus() != null && post.getStatus() == STATUS_DELETED) {
            return;
        }
        post.setStatus(STATUS_DELETED);
        postMapper.updateById(post);
        log.info("控制台删除帖子: {}", postId);
    }

    @Override
    @Transactional
    public void restorePostByAdmin(Long postId, String reason) {
        Post post = getPostOrThrow(postId);
        assertCanManagePost(post, reason, "restore");
        if (post.getStatus() == null || post.getStatus() != STATUS_DELETED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "帖子未处于删除状态");
        }
        post.setStatus(STATUS_NORMAL);
        postMapper.updateById(post);
        log.info("控制台恢复帖子: {}", postId);
    }

    @Override
    @Transactional
    public void purgePostByAdmin(Long postId, String reason) {
        Post post = getPostOrThrow(postId);
        assertCanManagePost(post, reason, "purge");
        if (post.getStatus() == null || post.getStatus() != STATUS_DELETED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "帖子未处于删除状态");
        }
        postMapper.deleteById(postId);
        log.info("控制台彻底删除帖子: {}", postId);
    }

    @Override
    public PostVO getPostDetail(Long postId) {
        Post post = getPostOrThrow(postId);

        // 已删除的帖子不可见
        if (post.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        List<String> boards = loadBoards(postId);
        return toPostVO(post, true, boards, null, false);
    }

    @Override
    @Transactional
    public boolean recordPostView(Long postId) {
        Post post = getPostOrThrow(postId);
        if (post.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        String key = VIEW_KEY_PREFIX + postId;
        Long added = redisTemplate.opsForSet().add(key, String.valueOf(userId));
        if (added != null && added > 0) {
            postMapper.incrementViewCount(postId);
            return true;
        }
        return false;
    }

    @Override
    public PageResult<PostVO> queryPosts(PostQueryDTO query) {
        return queryPostsInternal(query, true, false);
    }

    @Override
    public PageResult<PostVO> queryPostsForConsole(PostQueryDTO query) {
        return queryPostsInternal(query, false, true);
    }

    private PageResult<PostVO> queryPostsInternal(PostQueryDTO query, boolean defaultNormalStatus, boolean applyDataScope) {
        Page<Post> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Post> wrapper = buildWrapper(query, defaultNormalStatus);
        if (applyDataScope && query.getStatus() == null) {
            wrapper.ne(Post::getStatus, STATUS_DELETED);
        }
        if (applyDataScope) {
            applyPostDataScope(wrapper);
        }

        Page<Post> result = postMapper.selectPage(page, wrapper);

        List<Long> postIds = result.getRecords().stream()
                .map(Post::getId)
                .collect(Collectors.toList());
        Map<Long, List<String>> boardsMap = loadBoardsMap(postIds);
        PostUserContext context = buildPostUserContext(result.getRecords());

        List<PostVO> records = result.getRecords().stream()
                .map(post -> toPostVO(post, false, boardsMap.get(post.getId()), context, applyDataScope))
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private LambdaQueryWrapper<Post> buildWrapper(PostQueryDTO query, boolean defaultVisibleStatus) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        if (query.getStatus() != null) {
            wrapper.eq(Post::getStatus, query.getStatus());
        } else if (defaultVisibleStatus) {
            // 前端默认显示所有可见状态的帖子（正常、已解决、已售出）
            wrapper.in(Post::getStatus, VISIBLE_STATUSES);
        }

        String normalizedBoard = BoardUtil.normalizeBoardKey(query.getBoard());
        if (query.getBoard() != null && !query.getBoard().isEmpty() && normalizedBoard == null) {
            wrapper.eq(Post::getId, -1L);
            return wrapper;
        }
        if (normalizedBoard != null) {
            List<Long> postIds = postBoardMapper.selectList(
                    new LambdaQueryWrapper<PostBoard>()
                            .select(PostBoard::getPostId)
                            .eq(PostBoard::getBoard, normalizedBoard)
            ).stream().map(PostBoard::getPostId).collect(Collectors.toList());
            if (postIds.isEmpty()) {
                wrapper.eq(Post::getId, -1L);
                return wrapper;
            }
            wrapper.in(Post::getId, postIds);
        }

        if (query.getCategory() != null && !query.getCategory().isEmpty()) {
            wrapper.eq(Post::getCategory, query.getCategory());
        }

        if (query.getUserId() != null) {
            wrapper.eq(Post::getUserId, query.getUserId());
        }

        if (query.getShowOnHome() != null) {
            wrapper.eq(Post::getShowOnHome, query.getShowOnHome());
        }

        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Post::getTitle, keyword).or().like(Post::getContent, keyword));
        }

        String orderBy = query.getOrderBy();
        if ("hot".equals(orderBy)) {
            wrapper.orderByDesc(Post::getLastInteractionAt);
        } else {
            wrapper.orderByDesc(Post::getCreatedAt);
        }

        return wrapper;
    }

    private void applyPostDataScope(LambdaQueryWrapper<Post> wrapper) {
        Long userId = StpUtil.getLoginIdAsLong();
        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        if (scope.isAllowAll()) {
            return;
        }
        List<Long> allowedUserIds = dataScopeService.resolveAllowedUserIds(scope, userId);
        if (allowedUserIds.isEmpty()) {
            wrapper.eq(Post::getUserId, -1L);
            return;
        }
        wrapper.in(Post::getUserId, allowedUserIds);
    }

    private void assertCanManagePost(Post post, String reason, String action) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!dataScopeService.canAccessUser(operatorId, post.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该帖子");
        }
        if (!Objects.equals(operatorId, post.getUserId()) && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写操作原因");
        }
        operLogService.log(operatorId, null, "post", post.getId(), action, reason, null, null, null);
    }

    @Override
    @Transactional
    public void likePost(Long postId) {
        Long userId = StpUtil.getLoginIdAsLong();
        getPostOrThrow(postId);

        // 检查是否已点赞
        Like existingLike = likeMapper.selectOne(
                new LambdaQueryWrapper<Like>()
                        .eq(Like::getUserId, userId)
                        .eq(Like::getPostId, postId)
        );

        if (existingLike != null) {
            return; // 已点赞，忽略
        }

        // 添加点赞记录
        Like like = new Like();
        like.setUserId(userId);
        like.setPostId(postId);
        likeMapper.insert(like);

        // 更新点赞数
        postMapper.updateLikeCount(postId, 1);
        postMapper.updateLastInteractionAt(postId);
    }

    @Override
    @Transactional
    public void unlikePost(Long postId) {
        Long userId = StpUtil.getLoginIdAsLong();

        int deleted = likeMapper.delete(
                new LambdaQueryWrapper<Like>()
                        .eq(Like::getUserId, userId)
                        .eq(Like::getPostId, postId)
        );

        if (deleted > 0) {
            postMapper.updateLikeCount(postId, -1);
        }
    }

    @Override
    @Transactional
    public void bookmarkPost(Long postId) {
        Long userId = StpUtil.getLoginIdAsLong();
        getPostOrThrow(postId);

        // 检查是否已收藏
        Bookmark existing = bookmarkMapper.selectOne(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .eq(Bookmark::getPostId, postId)
        );

        if (existing != null) {
            return;
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(userId);
        bookmark.setPostId(postId);
        bookmarkMapper.insert(bookmark);
    }

    @Override
    @Transactional
    public BatchActionResultVO bookmarkPosts(List<Long> postIds) {
        Long userId = StpUtil.getLoginIdAsLong();
        BatchActionResultVO result = new BatchActionResultVO();

        if (postIds == null || postIds.isEmpty()) {
            result.setRequested(0);
            result.setSuccess(0);
            result.setSkipped(0);
            return result;
        }

        List<Long> uniqueIds = postIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        result.setRequested(uniqueIds.size());

        if (uniqueIds.isEmpty()) {
            result.setSuccess(0);
            result.setSkipped(0);
            return result;
        }

        List<Post> posts = postMapper.selectBatchIds(uniqueIds);
        Set<Long> validIds = posts.stream()
                .filter(post -> post.getStatus() == null || post.getStatus() != STATUS_DELETED)
                .map(Post::getId)
                .collect(Collectors.toSet());

        if (validIds.isEmpty()) {
            result.setSuccess(0);
            result.setSkipped(result.getRequested());
            return result;
        }

        List<Bookmark> existing = bookmarkMapper.selectList(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .in(Bookmark::getPostId, validIds)
        );
        Set<Long> existingIds = existing.stream()
                .map(Bookmark::getPostId)
                .collect(Collectors.toSet());

        int successCount = 0;
        for (Long postId : validIds) {
            if (existingIds.contains(postId)) {
                continue;
            }
            Bookmark bookmark = new Bookmark();
            bookmark.setUserId(userId);
            bookmark.setPostId(postId);
            bookmarkMapper.insert(bookmark);
            successCount++;
        }

        result.setSuccess(successCount);
        result.setSkipped(result.getRequested() - successCount);
        return result;
    }

    @Override
    @Transactional
    public void unbookmarkPost(Long postId) {
        Long userId = StpUtil.getLoginIdAsLong();

        bookmarkMapper.delete(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .eq(Bookmark::getPostId, postId)
        );
    }

    @Override
    public PageResult<PostVO> getUserBookmarks(Long userId, int page, int size) {
        Page<Bookmark> bookmarkPage = new Page<>(page, size);

        Page<Bookmark> bookmarks = bookmarkMapper.selectPage(
                bookmarkPage,
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .orderByDesc(Bookmark::getCreatedAt)
        );

        List<Long> postIds = bookmarks.getRecords().stream()
                .map(Bookmark::getPostId)
                .collect(Collectors.toList());

        if (postIds.isEmpty()) {
            return PageResult.empty();
        }

        List<Post> posts = postMapper.selectBatchIds(postIds);
        List<Long> filteredPostIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());
        Map<Long, List<String>> boardsMap = loadBoardsMap(filteredPostIds);
        PostUserContext context = buildPostUserContext(posts);

        List<PostVO> records = posts.stream()
                .filter(p -> p.getStatus() != STATUS_DELETED)
                .map(post -> toPostVO(post, false, boardsMap.get(post.getId()), context, false))
                .collect(Collectors.toList());

        return PageResult.of(records, bookmarks.getTotal(), bookmarks.getCurrent(), bookmarks.getSize());
    }

    @Override
    @Transactional
    public void markAsResolved(Long postId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Post post = getPostOrThrow(postId);

        // 权限校验：仅作者可标记
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此帖子");
        }

        post.setStatus(STATUS_RESOLVED);
        postMapper.updateById(post);

        log.info("用户 {} 标记帖子 {} 为已解决", userId, postId);
    }

    @Override
    @Transactional
    public void markAsResolvedByAdmin(Long postId, String reason) {
        Post post = getPostOrThrow(postId);
        assertCanManagePost(post, reason, "resolve");
        if (post.getStatus() != null && post.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已删除的帖子无法标记");
        }
        post.setStatus(STATUS_RESOLVED);
        postMapper.updateById(post);
        log.info("控制台标记帖子 {} 为已解决", postId);
    }

    @Override
    @Transactional
    public void markAsSold(Long postId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Post post = getPostOrThrow(postId);

        // 权限校验：仅作者可标记
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此帖子");
        }

        // 验证帖子是否属于市集板块
        List<String> boards = loadBoardsMap(List.of(postId)).get(postId);
        if (boards == null || !boards.contains(BOARD_MARKET)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有市集帖子可以标记为已售出");
        }

        post.setStatus(STATUS_SOLD);
        postMapper.updateById(post);

        log.info("用户 {} 标记帖子 {} 为已售出", userId, postId);
    }

    @Override
    @Transactional
    public void markAsSoldByAdmin(Long postId, String reason) {
        Post post = getPostOrThrow(postId);
        assertCanManagePost(post, reason, "sold");
        if (post.getStatus() != null && post.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已删除的帖子无法标记");
        }
        post.setStatus(STATUS_SOLD);
        postMapper.updateById(post);
        log.info("控制台标记帖子 {} 为已售出", postId);
    }

    @Override
    public PageResult<PostVO> searchPosts(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return PageResult.empty();
        }

        Page<Post> postPage = new Page<>(page, size);
        IPage<Post> result = postMapper.fullTextSearch(postPage, keyword.trim());

        List<Long> postIds = result.getRecords().stream()
                .map(Post::getId)
                .collect(Collectors.toList());
        Map<Long, List<String>> boardsMap = loadBoardsMap(postIds);
        PostUserContext context = buildPostUserContext(result.getRecords());

        List<PostVO> records = result.getRecords().stream()
                .map(post -> toPostVO(post, false, boardsMap.get(post.getId()), context, false))
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Post getPostOrThrow(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        return post;
    }

    private PostVO toPostVO(Post post, boolean includeFiles, List<String> boards, PostUserContext context, boolean revealAnonymous) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);
        if (boards != null && !boards.isEmpty()) {
            vo.setBoards(boards);
            vo.setBoard(boards.get(0));
        } else {
            vo.setBoards(List.of());
        }

        // 处理作者信息
        if (Boolean.TRUE.equals(post.getIsAnonymous()) && !revealAnonymous) {
            // 匿名帖子不显示作者信息
            vo.setAuthor(null);
        } else {
            User user = null;
            if (context != null && context.userMap != null && post.getUserId() != null) {
                user = context.userMap.get(post.getUserId());
            }
            if (user == null) {
                user = userMapper.selectById(post.getUserId());
            }
            if (user != null) {
                UserVO userVO = new UserVO();
                userVO.setId(user.getId());
                userVO.setUsername(user.getUsername());
                userVO.setNickname(user.getNickname());
                userVO.setAvatar(user.getAvatar());
                userVO.setVerifyStatus(user.getVerifyStatus());
                vo.setAuthor(userVO);
            }
        }

        // 处理当前用户状态
        if (context != null) {
            vo.setIsLiked(context.likedPostIds != null && context.likedPostIds.contains(post.getId()));
            vo.setIsBookmarked(context.bookmarkedPostIds != null && context.bookmarkedPostIds.contains(post.getId()));
        } else if (StpUtil.isLogin()) {
            Long currentUserId = StpUtil.getLoginIdAsLong();

            Like like = likeMapper.selectOne(
                    new LambdaQueryWrapper<Like>()
                            .eq(Like::getUserId, currentUserId)
                            .eq(Like::getPostId, post.getId())
            );
            vo.setIsLiked(like != null);

            Bookmark bookmark = bookmarkMapper.selectOne(
                    new LambdaQueryWrapper<Bookmark>()
                            .eq(Bookmark::getUserId, currentUserId)
                            .eq(Bookmark::getPostId, post.getId())
            );
            vo.setIsBookmarked(bookmark != null);
        } else {
            vo.setIsLiked(false);
            vo.setIsBookmarked(false);
        }

        // 加载附件
        if (includeFiles) {
            List<FileVO> files = fileService.getFilesByTarget(post.getId(), "post");
            vo.setFiles(files);
        }

        return vo;
    }

    private PostUserContext buildPostUserContext(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return new PostUserContext(Collections.emptyMap(), Collections.emptySet(), Collections.emptySet());
        }

        Map<Long, User> userMap = new HashMap<>();
        Set<Long> userIds = posts.stream()
                .map(Post::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                if (user != null) {
                    userMap.put(user.getId(), user);
                }
            }
        }

        Set<Long> likedPostIds = Collections.emptySet();
        Set<Long> bookmarkedPostIds = Collections.emptySet();
        if (StpUtil.isLogin()) {
            Long currentUserId = StpUtil.getLoginIdAsLong();
            List<Long> postIds = posts.stream()
                    .map(Post::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!postIds.isEmpty()) {
                List<Like> likes = likeMapper.selectList(
                        new LambdaQueryWrapper<Like>()
                                .eq(Like::getUserId, currentUserId)
                                .in(Like::getPostId, postIds)
                );
                likedPostIds = likes.stream()
                        .map(Like::getPostId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                List<Bookmark> bookmarks = bookmarkMapper.selectList(
                        new LambdaQueryWrapper<Bookmark>()
                                .eq(Bookmark::getUserId, currentUserId)
                                .in(Bookmark::getPostId, postIds)
                );
                bookmarkedPostIds = bookmarks.stream()
                        .map(Bookmark::getPostId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
        }

        return new PostUserContext(userMap, likedPostIds, bookmarkedPostIds);
    }

    private void savePostBoards(Long postId, List<String> boards) {
        if (boards == null || boards.isEmpty()) {
            return;
        }
        for (String board : boards) {
            PostBoard postBoard = new PostBoard();
            postBoard.setPostId(postId);
            postBoard.setBoard(board);
            postBoardMapper.insert(postBoard);
        }
    }

    private void replacePostBoards(Long postId, List<String> boards) {
        postBoardMapper.delete(new LambdaQueryWrapper<PostBoard>()
                .eq(PostBoard::getPostId, postId));
        savePostBoards(postId, boards);
    }

    private List<String> loadBoards(Long postId) {
        List<PostBoard> records = postBoardMapper.selectList(
                new LambdaQueryWrapper<PostBoard>()
                        .eq(PostBoard::getPostId, postId)
        );
        if (records.isEmpty()) {
            return List.of();
        }
        List<String> boards = new ArrayList<>();
        for (PostBoard record : records) {
            boards.add(record.getBoard());
        }
        return boards;
    }

    private Map<Long, List<String>> loadBoardsMap(List<Long> postIds) {
        Map<Long, List<String>> map = new HashMap<>();
        if (postIds == null || postIds.isEmpty()) {
            return map;
        }
        List<PostBoard> records = postBoardMapper.selectList(
                new LambdaQueryWrapper<PostBoard>()
                        .in(PostBoard::getPostId, postIds)
        );
        for (PostBoard record : records) {
            map.computeIfAbsent(record.getPostId(), key -> new ArrayList<>())
                    .add(record.getBoard());
        }
        return map;
    }
}
