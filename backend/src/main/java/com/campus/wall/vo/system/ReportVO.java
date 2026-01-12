package com.campus.wall.vo.system;

import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "举报视图")
public class ReportVO {

    private Long id;
    private String reason;
    private Integer status;
    private String result;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;

    private UserVO reporter;
    private PostVO post;
    private UserVO handler;
}
