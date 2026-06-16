package com.campus.wall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.file.cleanup")
public class FileCleanupProperties {

    /**
     * 未绑定超过多少小时后标记为待清理
     */
    private int markOrphanHours = 24;

    /**
     * 标记后保留天数
     */
    private int retainDays = 7;

    /**
     * 单次删除上限
     */
    private int deleteLimit = 200;
}
