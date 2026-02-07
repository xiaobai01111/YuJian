package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.wall.config.typehandler.JsonbLongListTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证规则实体
 */
@Data
@SuppressWarnings("serial")
@TableName(value = "sys_auth_rules", autoResultMap = true)
public class SysAuthRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Boolean enabled;

    private String triggerType;

    private String verifyMethod;

    private String matchType;

    private String matchValue;

    @TableField(typeHandler = JsonbLongListTypeHandler.class, jdbcType = org.apache.ibatis.type.JdbcType.OTHER)
    private List<Long> roleIds;

    private Integer priority;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
