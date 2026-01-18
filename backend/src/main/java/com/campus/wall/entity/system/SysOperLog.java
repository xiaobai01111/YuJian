package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.wall.config.typehandler.JsonbTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计日志实体
 */
@Data
@TableName(value = "sys_oper_log", autoResultMap = true)
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;

    private String operatorName;

    private String targetType;

    private Long targetId;

    private String action;

    private String reason;

    @TableField(typeHandler = JsonbTypeHandler.class, jdbcType = org.apache.ibatis.type.JdbcType.OTHER)
    private Object beforeValue;

    @TableField(typeHandler = JsonbTypeHandler.class, jdbcType = org.apache.ibatis.type.JdbcType.OTHER)
    private Object afterValue;

    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
