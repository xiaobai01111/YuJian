package com.campus.wall.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量收藏请求")
public class PostBatchBookmarkDTO {

    @NotEmpty(message = "帖子ID不能为空")
    private List<Long> postIds;
}
