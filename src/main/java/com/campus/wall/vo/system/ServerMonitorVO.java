package com.campus.wall.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 服务监控信息
 */
@Data
public class ServerMonitorVO {

    @Schema(description = "CPU信息")
    private CpuInfo cpu;

    @Schema(description = "物理内存信息")
    private MemoryInfo memory;

    @Schema(description = "JVM内存信息")
    private JvmInfo jvm;

    @Schema(description = "服务器信息")
    private ServerInfo server;

    @Schema(description = "Java信息")
    private JavaInfo javaInfo;

    @Schema(description = "磁盘信息")
    private List<DiskInfo> disks;

    @Data
    public static class CpuInfo {
        @Schema(description = "核心数")
        private int coreCount;
        @Schema(description = "用户使用率")
        private double userUsage;
        @Schema(description = "系统使用率")
        private double systemUsage;
        @Schema(description = "总使用率")
        private double totalUsage;
        @Schema(description = "空闲率")
        private double idleUsage;
    }

    @Data
    public static class MemoryInfo {
        @Schema(description = "总内存")
        private String total;
        @Schema(description = "已用内存")
        private String used;
        @Schema(description = "剩余内存")
        private String free;
        @Schema(description = "使用率")
        private double usage;
    }

    @Data
    public static class JvmInfo {
        @Schema(description = "总内存")
        private String total;
        @Schema(description = "已用内存")
        private String used;
        @Schema(description = "剩余内存")
        private String free;
        @Schema(description = "最大内存")
        private String max;
        @Schema(description = "使用率")
        private double usage;
    }

    @Data
    public static class ServerInfo {
        @Schema(description = "服务器名称")
        private String hostName;
        @Schema(description = "服务器IP")
        private String hostIp;
        @Schema(description = "操作系统")
        private String osName;
        @Schema(description = "系统架构")
        private String osArch;
    }

    @Data
    public static class JavaInfo {
        @Schema(description = "Java名称")
        private String javaName;
        @Schema(description = "Java版本")
        private String javaVersion;
        @Schema(description = "启动时间")
        private String startTime;
        @Schema(description = "运行时长")
        private String runTime;
        @Schema(description = "安装路径")
        private String javaHome;
        @Schema(description = "项目路径")
        private String projectDir;
        @Schema(description = "启动参数")
        private String inputArgs;
    }

    @Data
    public static class DiskInfo {
        @Schema(description = "挂载点")
        private String mount;
        @Schema(description = "文件系统")
        private String fileSystem;
        @Schema(description = "盘符类型")
        private String diskType;
        @Schema(description = "总大小")
        private String total;
        @Schema(description = "可用大小")
        private String free;
        @Schema(description = "已用大小")
        private String used;
        @Schema(description = "使用率")
        private double usage;
    }
}
