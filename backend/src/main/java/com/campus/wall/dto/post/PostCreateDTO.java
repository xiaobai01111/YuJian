package com.campus.wall.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "创建帖子请求")
public class PostCreateDTO {

    @NotBlank(message = "板块不能为空")
    private String board;

    @Size(max = 200, message = "标题最多200个字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容最多10000个字符")
    private String content;

    private Boolean isAnonymous = false;

    private String category;

    private BigDecimal price;

    private String location;

    private LocalDateTime lostTime;

    private List<Long> fileIds;
}
