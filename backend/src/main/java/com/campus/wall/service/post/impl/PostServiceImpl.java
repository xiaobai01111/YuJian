package com.campus.wall.service.post.impl;

import cn.dev33.satoken.stp.StpUtil;
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
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.BookmarkMapper;
import com.campus.wall.mapper.post.LikeMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.content.SensitiveWordService;
import com.campus.wall.service.file.FileService;
import com.campus.wall.service.post.PostService;
import com.campus.wall.vo.file.FileVO;
import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
    private final UserMapper userMapper;
    private final FileService fileService;
    private final SensitiveWordService sensitiveWordService;

    // 帖子状态：0正常 1已解决 2已删除
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_RESOLVED = 1;
    private static final int STATUS_DELETED = 2;

    // 树洞板块强制匿名
    private static final String BOARD_TREE_HOLE = "tree-hole";

    @Override
    @Transactional
    public Long createPost(PostCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 敏感词检测
        if (sensitiveWordService.containsSensitiveWord(dto.getTitle())) {
            throw new BusinessException(ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD, "标题包含敏感词");
        }
        if (sensitiveWordService.containsSensitiveWord(dto.getContent())) {
            throw new BusinessException(ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD, "内容包含敏感词");
        }

        // 树洞板块强制匿名
        Boolean isAnonymous = dto.getIsAnonymous();
        if (BOARD_TREE_HOLE.equals(dto.getBoard())) {
            isAnonymous = true;
        }

        // 创建帖子
        Post post = new Post();
        post.setUserId(userId);
        post.setBoard(dto.getBoard());
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setIsAnonymous(isAnonymous);
        post.setCategory(dto.getCategory());
        post.setPrice(dto.getPrice());
        post.setLocation(dto.getLocation());
        post.setLostTime(dto.getLostTime());
        post.setStatus(STATUS_NORMAL);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setLastInteractionAt(LocalDateTime.now());

        postMapper.insert(post);

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
        boolean isAdmin = StpUtil.hasRole("admin");
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

        return toPostVO(post, true);
    }

    @Override
    public PageResult<PostVO> queryPosts(PostQueryDTO query) {
        Page<Post> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        // 排除已删除
        wrapper.ne(Post::getStatus, STATUS_DELETED);

        // 板块筛选
        if (query.getBoard() != null && !query.getBoard().isEmpty()) {
            wrapper.eq(Post::getBoard, query.getBoard());
        }

        // 分类筛选
        if (query.getCategory() != null && !query.getCategory().isEmpty()) {
            wrapper.eq(Post::getCategory, query.getCategory());
        }

        // 状态筛选
        if (query.getStatus() != null) {
            wrapper.eq(Post::getStatus, query.getStatus());
        }

        // 用户筛选
        if (query.getUserId() != null) {
            wrapper.eq(Post::getUserId, query.getUserId());
        }

        // 排序
        String orderBy = query.getOrderBy();
        if ("hot".equals(orderBy)) {
            wrapper.orderByDesc(Post::getLastInteractionAt);
        } else {
            wrapper.orderByDesc(Post::getCreatedAt);
        }

        Page<Post> result = postMapper.selectPage(page, wrapper);

        List<PostVO> records = result.getRecords().stream()
                .map(post -> toPostVO(post, false))
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
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
        List<PostVO> records = posts.stream()
                .filter(p -> p.getStatus() != STATUS_DELETED)
                .map(post -> toPostVO(post, false))
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

        List<PostVO> records = result.getRecords().stream()
                .map(post -> toPostVO(post, false))
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

    private PostVO toPostVO(Post post, boolean includeFiles) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);

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
}
