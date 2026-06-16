package com.campus.wall.dto.post;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CommentBatchDeleteDTO {

    @NotEmpty(message = "评论ID不能为空")
    private List<Long> ids;

    private String reason;
}
