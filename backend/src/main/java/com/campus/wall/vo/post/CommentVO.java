package com.campus.wall.vo.post;

import com.campus.wall.vo.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "评论视图")
public class CommentVO {

    private Long id;
    private Long postId;
    private Long parentId;
    private String content;
    private String anonymousId;
    private Boolean isOwner;
    private LocalDateTime createdAt;

    @Schema(description = "评论者（匿名帖子显示anonymousId）")
    private UserVO author;

    @Schema(description = "子评论列表")
    private List<CommentVO> children;
}
