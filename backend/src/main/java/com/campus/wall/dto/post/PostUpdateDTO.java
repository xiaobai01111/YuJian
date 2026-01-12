package com.campus.wall.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "更新帖子请求")
public class PostUpdateDTO {

    @Size(max = 200, message = "标题最多200个字符")
    private String title;

    @Size(max = 10000, message = "内容最多10000个字符")
    private String content;

    private String category;

    private BigDecimal price;

    private String location;

    private LocalDateTime lostTime;

    private List<Long> addFileIds;

    private List<Long> removeFileIds;
}
