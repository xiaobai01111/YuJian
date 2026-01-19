package com.campus.wall.vo.post;

import com.campus.wall.vo.user.UserVO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentConsoleVO {

    private Long id;

    private Long postId;

    private String content;

    private Integer status;

    private LocalDateTime createdAt;

    private UserVO author;
}
