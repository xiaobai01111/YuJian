package com.campus.wall.vo.post;

import com.campus.wall.vo.file.FileVO;
import com.campus.wall.vo.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "帖子视图")
public class PostVO {

    private Long id;
    private String board;
    @Schema(description = "板块标识列表")
    private List<String> boards;
    private String title;
    private String content;
    private Boolean isAnonymous;
    private String category;
    private BigDecimal price;
    private String location;
    private LocalDateTime lostTime;
    private Integer status;
    @Schema(description = "是否同步到首页展示")
    private Boolean showOnHome;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private LocalDateTime createdAt;

    @Schema(description = "作者信息（匿名时为null）")
    private UserVO author;

    @Schema(description = "当前用户是否已点赞")
    private Boolean isLiked;

    @Schema(description = "当前用户是否已收藏")
    private Boolean isBookmarked;

    @Schema(description = "附件列表")
    private List<FileVO> files;
}
