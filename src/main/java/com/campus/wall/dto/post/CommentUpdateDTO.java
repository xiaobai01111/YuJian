package com.campus.wall.dto.post;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentUpdateDTO {

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
