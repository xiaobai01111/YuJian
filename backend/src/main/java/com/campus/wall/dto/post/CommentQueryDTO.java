package com.campus.wall.dto.post;

import com.campus.wall.dto.common.PageQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 控制台评论查询参数
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CommentQueryDTO extends PageQueryDTO {

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "状态：0正常 1已删除")
    private Integer status;
}
