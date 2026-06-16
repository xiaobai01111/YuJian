package com.campus.wall.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Redis监控信息
 */
@Data
public class RedisMonitorVO {

    @Schema(description = "基础信息")
    private BasicInfo basic;

    @Schema(description = "内存信息")
    private MemoryInfo memory;

    @Schema(description = "命令统计")
    private List<CommandStat> commandStats;

    @Data
    public static class BasicInfo {
        @Schema(description = "Redis版本")
        private String version;
        @Schema(description = "运行模式")
        private String runMode;
        @Schema(description = "端口")
        private String port;
        @Schema(description = "客户端数")
        private String connectedClients;
        @Schema(description = "运行时间(天)")
        private String uptimeDays;
        @Schema(description = "使用内存")
        private String usedMemory;
        @Schema(description = "使用CPU")
        private String usedCpu;
        @Schema(description = "内存配置")
        private String maxMemory;
        @Schema(description = "AOF是否开启")
        private String aofEnabled;
        @Schema(description = "RDB是否成功")
        private String rdbStatus;
        @Schema(description = "键数量")
        private String keyCount;
        @Schema(description = "网络入口")
        private String networkInput;
        @Schema(description = "网络出口")
        private String networkOutput;
    }

    @Data
    public static class MemoryInfo {
        @Schema(description = "使用内存")
        private String used;
        @Schema(description = "峰值内存")
        private String usedPeak;
        @Schema(description = "常驻内存")
        private String usedRss;
        @Schema(description = "Lua内存")
        private String usedLua;
        @Schema(description = "碎片率")
        private String fragmentationRatio;
        @Schema(description = "最大内存")
        private String max;
    }

    @Data
    public static class CommandStat {
        @Schema(description = "命令")
        private String command;
        @Schema(description = "调用次数")
        private Long calls;
        @Schema(description = "总耗时(微秒)")
        private Long usec;
        @Schema(description = "平均耗时(微秒)")
        private Double usecPerCall;
    }
}
