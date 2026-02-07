package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.wall.config.typehandler.JsonbLongListTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_notice")
public class SysNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /**
     * 状态：0草稿 1已发布 2已下线
     */
    private Integer status;

    /**
     * 可见范围：ALL全校 DEPT部门 USERS指定用户
     */
    private String scopeType;

    /**
     * 部门ID或用户ID列表，JSON数组格式
     */
    @TableField(typeHandler = JsonbLongListTypeHandler.class, jdbcType = JdbcType.OTHER)
    private List<Long> scopeIds;

    private Boolean isPinned;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private LocalDateTime publishedAt;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String createdByName;
}
