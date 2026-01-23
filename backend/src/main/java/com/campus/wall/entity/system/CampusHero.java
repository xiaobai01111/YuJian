package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.wall.config.typehandler.JsonbStringListTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "campus_heroes", autoResultMap = true)
public class CampusHero implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String pageKey;

    private String pageName;

    private Boolean enabled;

    private String theme;

    private String titleStart;

    private String titleHighlight;

    private String description;

    private String badge;

    private String primaryBtnText;

    private String primaryBtnLink;

    private String secondaryBtnText;

    private String secondaryBtnLink;

    private Boolean showStats;

    private String statsNumber;

    private String statsLabel;

    @TableField(typeHandler = JsonbStringListTypeHandler.class, jdbcType = org.apache.ibatis.type.JdbcType.OTHER)
    private List<String> avatarUrls;

    private String floatCardLabel;

    private String floatCardValue;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
