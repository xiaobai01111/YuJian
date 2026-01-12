package com.campus.wall.service.post.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.post.CommentCreateDTO;
import com.campus.wall.entity.post.Comment;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.CommentMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.content.AnonymousMappingService;
import com.campus.wall.service.content.SensitiveWordService;
import com.campus.wall.service.post.CommentService;
import com.campus.wall.vo.post.CommentVO;
import com.campus.wall.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final UserMapper userMapper;
    private final SensitiveWordService sensitiveWordService;
    private final AnonymousMappingService anonymousMappingService;

    // 评论状态：0正常 1已删除
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_DELETED = 1;

    // 树洞板块
    private static final String BOARD_TREE_HOLE = "tree-hole";

    @Override
    @Transactional
    public Long createComment(CommentCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

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
        if (BOARD_TREE_HOLE.equals(post.getBoard()) || Boolean.TRUE.equals(post.getIsAnonymous())) {
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
        boolean isAdmin = StpUtil.hasRole("admin");
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
    public List<CommentVO> getPostComments(Long postId) {
        // 获取帖子信息
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        // 查询所有评论
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .eq(Comment::getStatus, STATUS_NORMAL)
                        .orderByAsc(Comment::getCreatedAt)
        );

        // 转换为树形结构
        return buildCommentTree(comments, post);
    }

    @Override
    public PageResult<CommentVO> getPostCommentsPage(Long postId, int page, int size) {
        // 获取帖子信息
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        Page<Comment> commentPage = new Page<>(page, size);

        // 只查询一级评论
        Page<Comment> result = commentMapper.selectPage(
                commentPage,
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .isNull(Comment::getParentId)
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

        // 转换为 VO
        Map<Long, List<Comment>> finalChildrenMap = childrenMap;
        List<CommentVO> records = result.getRecords().stream()
                .map(comment -> {
                    CommentVO vo = toCommentVO(comment, post);
                    List<Comment> commentChildren = finalChildrenMap.get(comment.getId());
                    if (commentChildren != null) {
                        vo.setChildren(commentChildren.stream()
                                .map(c -> toCommentVO(c, post))
                                .collect(Collectors.toList()));
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private List<CommentVO> buildCommentTree(List<Comment> comments, Post post) {
        // 分离一级评论和子评论
        Map<Long, List<Comment>> childrenMap = comments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        List<CommentVO> rootComments = new ArrayList<>();
        for (Comment comment : comments) {
            if (comment.getParentId() == null) {
                CommentVO vo = toCommentVO(comment, post);
                vo.setChildren(buildChildren(comment.getId(), childrenMap, post));
                rootComments.add(vo);
            }
        }

        return rootComments;
    }

    private List<CommentVO> buildChildren(Long parentId, Map<Long, List<Comment>> childrenMap, Post post) {
        List<Comment> children = childrenMap.get(parentId);
        if (children == null) {
            return new ArrayList<>();
        }

        return children.stream()
                .map(c -> {
                    CommentVO vo = toCommentVO(c, post);
                    vo.setChildren(buildChildren(c.getId(), childrenMap, post));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private CommentVO toCommentVO(Comment comment, Post post) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setAnonymousId(comment.getAnonymousId());
        vo.setIsOwner(comment.getIsOwner());
        vo.setCreatedAt(comment.getCreatedAt());

        // 处理作者信息
        boolean isAnonymousPost = BOARD_TREE_HOLE.equals(post.getBoard()) 
                || Boolean.TRUE.equals(post.getIsAnonymous());

        if (isAnonymousPost) {
            // 匿名帖子不显示作者信息，使用 anonymousId
            vo.setAuthor(null);
        } else {
            User user = userMapper.selectById(comment.getUserId());
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
}
