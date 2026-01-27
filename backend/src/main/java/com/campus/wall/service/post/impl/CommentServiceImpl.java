package com.campus.wall.service.post.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.util.BoardUtil;
import com.campus.wall.util.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.constant.RateLimitConstants;
import com.campus.wall.dto.post.CommentBatchDeleteDTO;
import com.campus.wall.dto.post.CommentCreateDTO;
import com.campus.wall.dto.post.CommentQueryDTO;
import com.campus.wall.dto.post.CommentUpdateDTO;
import com.campus.wall.entity.post.Comment;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.post.PostBoard;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.CommentMapper;
import com.campus.wall.mapper.post.PostBoardMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.content.AnonymousMappingService;
import com.campus.wall.service.content.SensitiveWordService;
import com.campus.wall.service.post.CommentService;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.service.security.RateLimitService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.vo.post.CommentVO;
import com.campus.wall.vo.post.CommentConsoleVO;
import com.campus.wall.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final PostBoardMapper postBoardMapper;
    private final UserMapper userMapper;
    private final SensitiveWordService sensitiveWordService;
    private final AnonymousMappingService anonymousMappingService;
    private final RateLimitService rateLimitService;
    private final DataScopeService dataScopeService;
    private final OperLogService operLogService;

    // 评论状态：0正常 1已删除
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_DELETED = 1;

    // 树洞板块
    private static final String BOARD_TREE_HOLE = BoardUtil.BOARD_TREE_HOLE;

    @Override
    @Transactional
    public Long createComment(CommentCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        rateLimitService.checkRateLimit(
            "rate:comment:user:" + userId,
            RateLimitConstants.COMMENT_LIMIT_PER_MINUTE,
            RateLimitConstants.WINDOW_SECONDS,
            ResultCode.RATE_LIMIT_EXCEEDED
        );

        // 检查帖子是否存在
        Post post = postMapper.selectById(dto.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        // 敏感词检测
        if (sensitiveWordService.containsSensitiveWord(dto.getContent())) {
            throw new BusinessException(ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD, "评论包含敏感词");
        }

        // 检查父评论是否存在
        if (dto.getParentId() != null) {
            Comment parentComment = commentMapper.selectById(dto.getParentId());
            if (parentComment == null || !parentComment.getPostId().equals(dto.getPostId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "父评论不存在");
            }
        }

        // 判断是否为楼主
        boolean isOwner = post.getUserId().equals(userId);

        // 创建评论
        Comment comment = new Comment();
        comment.setPostId(dto.getPostId());
        comment.setUserId(userId);
        comment.setParentId(dto.getParentId());
        comment.setContent(dto.getContent());
        comment.setIsOwner(isOwner);
        comment.setStatus(STATUS_NORMAL);

        // 树洞帖子分配匿名标识
        if (isTreeHolePost(post) || Boolean.TRUE.equals(post.getIsAnonymous())) {
            if (isOwner) {
                comment.setAnonymousId("楼主");
            } else {
                // 生成匿名标识
                String anonymousTag = anonymousMappingService.generateAnonymousTag(userId, dto.getPostId());
                comment.setAnonymousId(anonymousTag);
            }
        }

        commentMapper.insert(comment);

        // 更新帖子评论数
        postMapper.updateCommentCount(dto.getPostId(), 1);
        postMapper.updateLastInteractionAt(dto.getPostId());

        log.info("用户 {} 在帖子 {} 发表评论: {}", userId, dto.getPostId(), comment.getId());
        return comment.getId();
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        Long userId = StpUtil.getLoginIdAsLong();

        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }

        // 权限校验：作者或管理员可删除
        boolean isAdmin = StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey());
        if (!comment.getUserId().equals(userId) && !isAdmin) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此评论");
        }

        // 软删除
        comment.setStatus(STATUS_DELETED);
        commentMapper.updateById(comment);

        // 更新帖子评论数
        postMapper.updateCommentCount(comment.getPostId(), -1);

        log.info("用户 {} 删除评论: {}", userId, commentId);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId, String reason) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        Post post = postMapper.selectById(comment.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (!dataScopeService.canAccessUser(operatorId, comment.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该评论");
        }
        if (!Objects.equals(operatorId, comment.getUserId()) && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写操作原因");
        }
        if (comment.getStatus() != null && comment.getStatus() == STATUS_DELETED) {
            return;
        }
        comment.setStatus(STATUS_DELETED);
        commentMapper.updateById(comment);
        postMapper.updateCommentCount(comment.getPostId(), -1);
        operLogService.log(operatorId, null, "comment", commentId, "delete", reason, null, null, null);
        log.info("管理员删除评论: {}", commentId);
    }

    @Override
    @Transactional
    public void deleteCommentsByAdmin(CommentBatchDeleteDTO dto) {
        if (dto == null || dto.getIds() == null || dto.getIds().isEmpty()) {
            return;
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        List<Comment> comments = commentMapper.selectBatchIds(dto.getIds());
        if (comments == null || comments.isEmpty()) {
            return;
        }

        Map<Long, Post> postMap = loadPostMap(comments);
        boolean needReason = comments.stream()
                .filter(Objects::nonNull)
                .anyMatch(comment -> !Objects.equals(operatorId, comment.getUserId()));
        if (needReason && !StringUtils.hasText(dto.getReason())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写操作原因");
        }

        Map<Long, Integer> postDecrement = new HashMap<>();
        for (Comment comment : comments) {
            if (comment == null) {
                continue;
            }
            Post post = postMap.get(comment.getPostId());
            if (post == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
            }
            if (!dataScopeService.canAccessUser(operatorId, comment.getUserId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该评论");
            }
            if (comment.getStatus() != null && comment.getStatus() == STATUS_DELETED) {
                continue;
            }
            comment.setStatus(STATUS_DELETED);
            commentMapper.updateById(comment);
            postDecrement.merge(comment.getPostId(), 1, Integer::sum);
            operLogService.log(operatorId, null, "comment", comment.getId(), "delete", dto.getReason(), null, null, null);
        }
        for (Map.Entry<Long, Integer> entry : postDecrement.entrySet()) {
            postMapper.updateCommentCount(entry.getKey(), -entry.getValue());
        }
        log.info("管理员批量删除评论: {}", dto.getIds().size());
    }

    @Override
    @Transactional
    public void updateCommentByAdmin(Long commentId, CommentUpdateDTO dto, String reason) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (comment.getStatus() != null && comment.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已删除的评论无法修改");
        }
        Post post = postMapper.selectById(comment.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (!dataScopeService.canAccessUser(operatorId, comment.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该评论");
        }
        if (!Objects.equals(operatorId, comment.getUserId()) && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写操作原因");
        }
        String content = dto.getContent() != null ? dto.getContent().trim() : "";
        if (content.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论内容不能为空");
        }
        if (sensitiveWordService.containsSensitiveWord(content)) {
            throw new BusinessException(ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD, "评论包含敏感词");
        }
        comment.setContent(content);
        commentMapper.updateById(comment);
        operLogService.log(operatorId, null, "comment", commentId, "update", reason, null, null, null);
        log.info("管理员修改评论: {}", commentId);
    }

    @Override
    public List<CommentVO> getPostComments(Long postId) {
        // 获取帖子信息
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        boolean isAnonymousPost = isTreeHolePost(post) || Boolean.TRUE.equals(post.getIsAnonymous());

        // 查询所有评论
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .eq(Comment::getStatus, STATUS_NORMAL)
                        .orderByAsc(Comment::getCreatedAt)
        );

        // 转换为树形结构
        Map<Long, User> userMap = loadUserMap(comments);
        return buildCommentTree(comments, post, isAnonymousPost, userMap);
    }

    @Override
    public PageResult<CommentVO> getPostCommentsPage(Long postId, int page, int size) {
        // 获取帖子信息
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        boolean isAnonymousPost = isTreeHolePost(post) || Boolean.TRUE.equals(post.getIsAnonymous());

        Page<Comment> commentPage = new Page<>(page, size);

        // 只查询一级评论
        Page<Comment> result = commentMapper.selectPage(
                commentPage,
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .and(wrapper -> wrapper.isNull(Comment::getParentId)
                                .or()
                                .eq(Comment::getParentId, 0L))
                        .eq(Comment::getStatus, STATUS_NORMAL)
                        .orderByAsc(Comment::getCreatedAt)
        );

        // 获取子评论
        List<Long> parentIds = result.getRecords().stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        Map<Long, List<Comment>> childrenMap = Map.of();
        if (!parentIds.isEmpty()) {
            List<Comment> children = commentMapper.selectList(
                    new LambdaQueryWrapper<Comment>()
                            .in(Comment::getParentId, parentIds)
                            .eq(Comment::getStatus, STATUS_NORMAL)
                            .orderByAsc(Comment::getCreatedAt)
            );
            childrenMap = children.stream()
                    .collect(Collectors.groupingBy(Comment::getParentId));
        }

        List<Comment> allComments = new ArrayList<>(result.getRecords());
        for (List<Comment> items : childrenMap.values()) {
            allComments.addAll(items);
        }
        Map<Long, User> userMap = loadUserMap(allComments);

        // 转换为 VO
        Map<Long, List<Comment>> finalChildrenMap = childrenMap;
        List<CommentVO> records = result.getRecords().stream()
                .map(comment -> {
                    CommentVO vo = toCommentVO(comment, post, isAnonymousPost, userMap);
                    List<Comment> commentChildren = finalChildrenMap.get(comment.getId());
                    if (commentChildren != null) {
                        vo.setChildren(commentChildren.stream()
                                .map(c -> toCommentVO(c, post, isAnonymousPost, userMap))
                                .collect(Collectors.toList()));
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<CommentConsoleVO> queryCommentsForConsole(CommentQueryDTO query) {
        Page<Comment> commentPage = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if (query.getPostId() != null) {
            wrapper.eq(Comment::getPostId, query.getPostId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Comment::getStatus, query.getStatus());
        } else {
            wrapper.eq(Comment::getStatus, STATUS_NORMAL);
        }
        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
            wrapper.like(Comment::getContent, query.getKeyword().trim());
        }
        wrapper.orderByDesc(Comment::getCreatedAt);

        applyCommentDataScope(wrapper);

        Page<Comment> result = commentMapper.selectPage(commentPage, wrapper);

        Map<Long, User> userMap = loadUserMap(result.getRecords());

        List<CommentConsoleVO> records = result.getRecords().stream()
                .map(comment -> {
                    CommentConsoleVO vo = new CommentConsoleVO();
                    vo.setId(comment.getId());
                    vo.setPostId(comment.getPostId());
                    vo.setContent(comment.getContent());
                    vo.setStatus(comment.getStatus());
                    vo.setCreatedAt(comment.getCreatedAt());

                    User user = userMap.get(comment.getUserId());
                    if (user != null) {
                        UserVO author = new UserVO();
                        author.setId(user.getId());
                        author.setUsername(user.getUsername());
                        author.setNickname(user.getNickname());
                        vo.setAuthor(author);
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void restoreCommentByAdmin(Long commentId, String reason) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (comment.getStatus() == null || comment.getStatus() != STATUS_DELETED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论未处于删除状态");
        }
        Post post = postMapper.selectById(comment.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (!dataScopeService.canAccessUser(operatorId, comment.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该评论");
        }
        if (!Objects.equals(operatorId, comment.getUserId()) && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写操作原因");
        }
        comment.setStatus(STATUS_NORMAL);
        commentMapper.updateById(comment);
        postMapper.updateCommentCount(comment.getPostId(), 1);
        operLogService.log(operatorId, null, "comment", commentId, "restore", reason, null, null, null);
        log.info("管理员恢复评论: {}", commentId);
    }

    @Override
    @Transactional
    public void purgeCommentByAdmin(Long commentId, String reason) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return;
        }
        if (comment.getStatus() == null || comment.getStatus() != STATUS_DELETED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论未处于删除状态");
        }
        Post post = postMapper.selectById(comment.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (!dataScopeService.canAccessUser(operatorId, comment.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该评论");
        }
        if (!Objects.equals(operatorId, comment.getUserId()) && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写操作原因");
        }
        commentMapper.deleteById(commentId);
        operLogService.log(operatorId, null, "comment", commentId, "purge", reason, null, null, null);
        log.info("管理员彻底删除评论: {}", commentId);
    }

    private void applyCommentDataScope(LambdaQueryWrapper<Comment> wrapper) {
        Long userId = StpUtil.getLoginIdAsLong();
        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        if (scope.isAllowAll()) {
            return;
        }
        List<Long> allowedUserIds = dataScopeService.resolveAllowedUserIds(scope, userId);
        if (allowedUserIds.isEmpty()) {
            wrapper.eq(Comment::getId, -1L);
            return;
        }
        wrapper.in(Comment::getUserId, allowedUserIds);
    }

    private List<CommentVO> buildCommentTree(List<Comment> comments, Post post, boolean isAnonymousPost, Map<Long, User> userMap) {
        // 分离一级评论和子评论
        Map<Long, List<Comment>> childrenMap = comments.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0L)
                .collect(Collectors.groupingBy(Comment::getParentId));

        List<CommentVO> rootComments = new ArrayList<>();
        for (Comment comment : comments) {
            if (comment.getParentId() == null || comment.getParentId() == 0L) {
                CommentVO vo = toCommentVO(comment, post, isAnonymousPost, userMap);
                vo.setChildren(buildChildren(comment.getId(), childrenMap, post, isAnonymousPost, userMap));
                rootComments.add(vo);
            }
        }

        return rootComments;
    }

    private List<CommentVO> buildChildren(Long parentId, Map<Long, List<Comment>> childrenMap, Post post, boolean isAnonymousPost, Map<Long, User> userMap) {
        List<Comment> children = childrenMap.get(parentId);
        if (children == null) {
            return new ArrayList<>();
        }

        return children.stream()
                .map(c -> {
                    CommentVO vo = toCommentVO(c, post, isAnonymousPost, userMap);
                    vo.setChildren(buildChildren(c.getId(), childrenMap, post, isAnonymousPost, userMap));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private CommentVO toCommentVO(Comment comment, Post post, boolean isAnonymousPost, Map<Long, User> userMap) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setAnonymousId(comment.getAnonymousId());
        vo.setIsOwner(comment.getIsOwner());
        vo.setCreatedAt(comment.getCreatedAt());

        // 处理作者信息
        if (isAnonymousPost) {
            // 匿名帖子不显示作者信息，使用 anonymousId
            vo.setAuthor(null);
        } else {
            User user = userMap != null ? userMap.get(comment.getUserId()) : null;
            if (user == null) {
                user = userMapper.selectById(comment.getUserId());
            }
            if (user != null) {
                UserVO userVO = new UserVO();
                userVO.setId(user.getId());
                userVO.setUsername(user.getUsername());
                userVO.setNickname(user.getNickname());
                userVO.setAvatar(user.getAvatar());
                vo.setAuthor(userVO);
            }
        }

        return vo;
    }

    private Map<Long, User> loadUserMap(List<Comment> comments) {
        Map<Long, User> map = new HashMap<>();
        if (comments == null || comments.isEmpty()) {
            return map;
        }
        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return map;
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        for (User user : users) {
            if (user != null) {
                map.put(user.getId(), user);
            }
        }
        return map;
    }

    private Map<Long, Post> loadPostMap(List<Comment> comments) {
        Map<Long, Post> map = new HashMap<>();
        if (comments == null || comments.isEmpty()) {
            return map;
        }
        List<Long> postIds = comments.stream()
                .map(Comment::getPostId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (postIds.isEmpty()) {
            return map;
        }
        List<Post> posts = postMapper.selectBatchIds(postIds);
        for (Post post : posts) {
            if (post != null) {
                map.put(post.getId(), post);
            }
        }
        return map;
    }

    private boolean isTreeHolePost(Post post) {
        if (post == null) {
            return false;
        }
        String normalizedBoard = BoardUtil.normalizeBoardKey(post.getBoard());
        if (BOARD_TREE_HOLE.equals(normalizedBoard)) {
            return true;
        }
        return postBoardMapper.selectCount(
                new LambdaQueryWrapper<PostBoard>()
                        .eq(PostBoard::getPostId, post.getId())
                        .eq(PostBoard::getBoard, BOARD_TREE_HOLE)
        ) > 0;
    }
}
