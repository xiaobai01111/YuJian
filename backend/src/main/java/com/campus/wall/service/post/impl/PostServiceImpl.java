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
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.content.SensitiveWordService;
import com.campus.wall.service.file.FileService;
import com.campus.wall.service.post.PostService;
import com.campus.wall.vo.file.FileVO;
import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.user.UserVO;
import com.campus.wall.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final SysRoleDeptMapper sysRoleDeptMapper;
    private final SysDeptMapper deptMapper;
    private final FileService fileService;
    private final SensitiveWordService sensitiveWordService;
    private final com.campus.wall.service.user.CreditService creditService;

    // 帖子状态：0正常 1已解决 2已删除
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_RESOLVED = 1;
    private static final int STATUS_DELETED = 2;

    // 板块常量
    private static final String BOARD_TREE_HOLE = BoardUtil.BOARD_TREE_HOLE;
    private static final String BOARD_MARKET = BoardUtil.BOARD_MARKET;

    @Override
    @Transactional
    public Long createPost(PostCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

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
    public PostVO getPostDetail(Long postId) {
        Post post = getPostOrThrow(postId);

        // 已删除的帖子不可见
        if (post.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        // 增加浏览数
        postMapper.incrementViewCount(postId);

        List<String> boards = loadBoards(postId);
        return toPostVO(post, true, boards);
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
        if (applyDataScope) {
            applyPostDataScope(wrapper);
        }

        Page<Post> result = postMapper.selectPage(page, wrapper);

        List<Long> postIds = result.getRecords().stream()
                .map(Post::getId)
                .collect(Collectors.toList());
        Map<Long, List<String>> boardsMap = loadBoardsMap(postIds);

        List<PostVO> records = result.getRecords().stream()
                .map(post -> toPostVO(post, false, boardsMap.get(post.getId())))
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private LambdaQueryWrapper<Post> buildWrapper(PostQueryDTO query, boolean defaultNormalStatus) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        if (query.getStatus() != null) {
            wrapper.eq(Post::getStatus, query.getStatus());
        } else if (defaultNormalStatus) {
            wrapper.eq(Post::getStatus, STATUS_NORMAL);
        }

        String normalizedBoard = BoardUtil.normalizeBoardKey(query.getBoard());
        if (query.getBoard() != null && !query.getBoard().isEmpty() && normalizedBoard == null) {
            wrapper.eq(Post::getId, -1L);
            return wrapper;
        }
        if (normalizedBoard != null) {
            wrapper.inSql(Post::getId, "SELECT post_id FROM post_boards WHERE board = '" + normalizedBoard + "'");
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
        if (SecurityUtil.isSuperAdmin()) {
            return;
        }

        List<Long> deptIds = sysRoleDeptMapper.selectDeptIdsByUserId(userId);
        if (deptIds == null) {
            deptIds = new ArrayList<>();
        }
        User user = userMapper.selectById(userId);
        if (user != null && user.getDeptId() != null) {
            if (!deptIds.contains(user.getDeptId())) {
                deptIds.add(user.getDeptId());
            }
        }

        if (deptIds.isEmpty()) {
            wrapper.eq(Post::getUserId, userId);
            return;
        }

        List<com.campus.wall.entity.system.SysDept> depts = deptMapper.selectBatchIds(deptIds);
        boolean allowAll = false;
        boolean allowSelf = false;
        List<Long> allowDeptIds = new ArrayList<>();
        boolean includeChildren = false;

        for (com.campus.wall.entity.system.SysDept dept : depts) {
            if (dept == null) continue;
            Integer scope = dept.getDataScope() != null ? dept.getDataScope() : SecurityConstants.DATA_SCOPE_DEPT;
            if (scope == SecurityConstants.DATA_SCOPE_ALL) {
                allowAll = true;
                break;
            }
            if (scope == SecurityConstants.DATA_SCOPE_SELF) {
                allowSelf = true;
            } else if (scope == SecurityConstants.DATA_SCOPE_DEPT) {
                allowDeptIds.add(dept.getId());
            } else if (scope == SecurityConstants.DATA_SCOPE_DEPT_AND_CHILD) {
                allowDeptIds.add(dept.getId());
                includeChildren = true;
            } else if (scope == SecurityConstants.DATA_SCOPE_CUSTOM) {
                allowDeptIds.add(dept.getId());
            }
        }

        if (allowAll) {
            return;
        }

        List<Long> scopedDeptIds = includeChildren ? expandDeptIds(allowDeptIds) : allowDeptIds;
        if (scopedDeptIds.isEmpty()) {
            if (allowSelf) {
                wrapper.eq(Post::getUserId, userId);
            } else {
                wrapper.eq(Post::getUserId, userId);
            }
            return;
        }

        String deptIdSql = scopedDeptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String inSql = "SELECT id FROM users WHERE deleted = 0 AND dept_id IN (" + deptIdSql + ")";

        if (allowSelf) {
            wrapper.and(w -> w.eq(Post::getUserId, userId).or().inSql(Post::getUserId, inSql));
        } else {
            wrapper.inSql(Post::getUserId, inSql);
        }
    }

    private List<Long> expandDeptIds(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        List<com.campus.wall.entity.system.SysDept> allDepts = deptMapper.selectList(null);
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (com.campus.wall.entity.system.SysDept dept : allDepts) {
            childrenMap.computeIfAbsent(dept.getParentId(), key -> new ArrayList<>()).add(dept.getId());
        }
        List<Long> result = new ArrayList<>();
        for (Long rootId : deptIds) {
            collectDeptChildren(rootId, childrenMap, result);
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    private void collectDeptChildren(Long deptId, Map<Long, List<Long>> childrenMap, List<Long> result) {
        if (deptId == null) {
            return;
        }
        result.add(deptId);
        List<Long> children = childrenMap.get(deptId);
        if (children != null) {
            for (Long childId : children) {
                collectDeptChildren(childId, childrenMap, result);
            }
        }
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

        List<PostVO> records = posts.stream()
                .filter(p -> p.getStatus() != STATUS_DELETED)
                .map(post -> toPostVO(post, false, boardsMap.get(post.getId())))
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

        List<PostVO> records = result.getRecords().stream()
                .map(post -> toPostVO(post, false, boardsMap.get(post.getId())))
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

    private PostVO toPostVO(Post post, boolean includeFiles, List<String> boards) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);
        if (boards != null && !boards.isEmpty()) {
            vo.setBoards(boards);
            vo.setBoard(boards.get(0));
        } else {
            vo.setBoards(List.of());
        }

        // 处理作者信息
        if (Boolean.TRUE.equals(post.getIsAnonymous())) {
            // 匿名帖子不显示作者信息
            vo.setAuthor(null);
        } else {
            User user = userMapper.selectById(post.getUserId());
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
        if (StpUtil.isLogin()) {
            Long currentUserId = StpUtil.getLoginIdAsLong();

            // 是否已点赞
            Like like = likeMapper.selectOne(
                    new LambdaQueryWrapper<Like>()
                            .eq(Like::getUserId, currentUserId)
                            .eq(Like::getPostId, post.getId())
            );
            vo.setIsLiked(like != null);

            // 是否已收藏
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
