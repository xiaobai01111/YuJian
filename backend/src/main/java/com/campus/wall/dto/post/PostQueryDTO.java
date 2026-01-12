package com.campus.wall.dto.post;

import com.campus.wall.dto.common.PageQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "帖子查询请求")
public class PostQueryDTO extends PageQueryDTO {

    private String board;
    private String category;
    private Integer status;
    private Long userId;
    private String keyword;
    private String orderBy = "latest";
    private String lostFoundType;
}
