package com.campus.wall.service.search.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.PageResult;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.post.PostBoard;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.PostBoardMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.search.SearchService;
import com.campus.wall.util.BoardUtil;
import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PostgreSQL 全文搜索实现
 * 使用 PostMapper.fullTextSearch 进行搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostgresSearchService implements SearchService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final PostBoardMapper postBoardMapper;

    // 帖子状态：0正常 1已解决 2已删除
    private static final int STATUS_DELETED = 2;

    // 树洞板块
    private static final String BOARD_TREE_HOLE = BoardUtil.BOARD_TREE_HOLE;

    @Override
    public PageResult<PostVO> search(String keyword, String board, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return PageResult.empty();
        }

        // 使用 MyBatis-Plus 分页
        Page<Post> postPage = new Page<>(page, size);

        // 使用 PostMapper 的全文搜索方法
        IPage<Post> result = postMapper.fullTextSearch(postPage, keyword.trim());

        String normalizedBoard = BoardUtil.normalizeBoardKey(board);
        if (board != null && !board.isEmpty() && normalizedBoard == null) {
            return PageResult.empty();
        }

        List<Long> postIds = result.getRecords().stream()
                .map(Post::getId)
                .collect(Collectors.toList());
        Map<Long, List<String>> boardsMap = loadBoardsMap(postIds);

        // 转换为 VO，排除已删除帖子，按板块过滤
        Map<Long, User> userMap = loadUserMap(result.getRecords());
        List<PostVO> records = result.getRecords().stream()
                .filter(p -> p.getStatus() != STATUS_DELETED)
                .filter(p -> normalizedBoard == null
                        || boardsMap.getOrDefault(p.getId(), List.of()).contains(normalizedBoard)
                        || normalizedBoard.equals(BoardUtil.normalizeBoardKey(p.getBoard())))
                .map(post -> toPostVO(post, boardsMap.get(post.getId()), userMap))
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public void indexPost(Long postId) {
        // PostgreSQL 全文搜索使用触发器自动更新 search_vector
        // 此方法为预留接口，用于切换到 Elasticsearch 等实现
        log.debug("PostgreSQL 全文搜索自动索引，无需手动操作: postId={}", postId);
    }

    @Override
    public void removeIndex(Long postId) {
        // PostgreSQL 全文搜索无需手动删除索引
        log.debug("PostgreSQL 全文搜索自动处理删除，无需手动操作: postId={}", postId);
    }

    @Override
    public void rebuildIndex() {
        // PostgreSQL 可通过 SQL 重建索引
        log.info("PostgreSQL 全文搜索索引重建请通过 SQL 执行");
    }

    private PostVO toPostVO(Post post, List<String> boards, Map<Long, User> userMap) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        if (boards != null && !boards.isEmpty()) {
            vo.setBoard(boards.getFirst());
            vo.setBoards(boards);
        } else {
            vo.setBoard(post.getBoard());
            vo.setBoards(List.of());
        }
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setCategory(post.getCategory());
        vo.setPrice(post.getPrice());
        vo.setLocation(post.getLocation());
        vo.setLostTime(post.getLostTime());
        vo.setStatus(post.getStatus());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setViewCount(post.getViewCount());
        vo.setCreatedAt(post.getCreatedAt());

        // 匿名帖子隐藏作者信息
        boolean isAnonymous = (boards != null && boards.contains(BOARD_TREE_HOLE))
                || Boolean.TRUE.equals(post.getIsAnonymous());

        if (!isAnonymous && post.getUserId() != null) {
            User user = userMap != null ? userMap.get(post.getUserId()) : null;
            if (user == null) {
                user = userMapper.selectById(post.getUserId());
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

    private Map<Long, List<String>> loadBoardsMap(List<Long> postIds) {
        Map<Long, List<String>> map = new HashMap<>();
        if (postIds == null || postIds.isEmpty()) {
            return map;
        }
        List<PostBoard> records = postBoardMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PostBoard>()
                        .in(PostBoard::getPostId, postIds)
        );
        for (PostBoard record : records) {
            map.computeIfAbsent(record.getPostId(), key -> new ArrayList<>())
                    .add(record.getBoard());
        }
        return map;
    }

    private Map<Long, User> loadUserMap(List<Post> posts) {
        Map<Long, User> map = new HashMap<>();
        if (posts == null || posts.isEmpty()) {
            return map;
        }
        List<Long> userIds = posts.stream()
                .map(Post::getUserId)
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
}
