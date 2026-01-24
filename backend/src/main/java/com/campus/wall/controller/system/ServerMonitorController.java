package com.campus.wall.controller.system;

import com.campus.wall.common.R;
import com.campus.wall.util.FileUtil;
import com.campus.wall.vo.system.ServerMonitorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务监控控制器
 */
@RestController
@RequestMapping("/api/v1/console/monitor/server")
@RequiredArgsConstructor
public class ServerMonitorController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    public R<ServerMonitorVO> getServerInfo() {
        ServerMonitorVO vo = new ServerMonitorVO();
        vo.setCpu(buildCpuInfo());
        vo.setMemory(buildMemoryInfo());
        vo.setJvm(buildJvmInfo());
        vo.setServer(buildServerInfo());
        vo.setJavaInfo(buildJavaInfo());
        vo.setDisks(buildDiskInfo());
        return R.ok(vo);
    }

    private ServerMonitorVO.CpuInfo buildCpuInfo() {
        ServerMonitorVO.CpuInfo cpu = new ServerMonitorVO.CpuInfo();
        var osBean = ManagementFactory.getOperatingSystemMXBean();
        int cores = osBean.getAvailableProcessors();
        cpu.setCoreCount(cores);

        double systemUsage = 0.0;
        double userUsage = 0.0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean bean) {
            systemUsage = percent(bean.getCpuLoad());
            userUsage = percent(bean.getProcessCpuLoad());
        }
        double totalUsage = Math.max(systemUsage, userUsage);
        double systemOnly = Math.max(systemUsage - userUsage, 0.0);
        double idle = Math.max(100.0 - totalUsage, 0.0);

        cpu.setUserUsage(round(userUsage));
        cpu.setSystemUsage(round(systemOnly));
        cpu.setTotalUsage(round(totalUsage));
        cpu.setIdleUsage(round(idle));
        return cpu;
    }

    private ServerMonitorVO.MemoryInfo buildMemoryInfo() {
        ServerMonitorVO.MemoryInfo memory = new ServerMonitorVO.MemoryInfo();
        var osBean = ManagementFactory.getOperatingSystemMXBean();
        long total = 0;
        long free = 0;
        long available = 0;
        long[] memInfo = readLinuxMemInfo();
        if (memInfo != null) {
            total = memInfo[0];
            available = memInfo[1];
            free = available;
        } else if (osBean instanceof com.sun.management.OperatingSystemMXBean bean) {
            total = bean.getTotalMemorySize();
            free = bean.getFreeMemorySize();
        }
        long used = Math.max(total - free, 0);
        memory.setTotal(FileUtil.formatFileSize(total));
        memory.setUsed(FileUtil.formatFileSize(used));
        memory.setFree(FileUtil.formatFileSize(free));
        memory.setUsage(round(percent(used, total)));
        return memory;
    }

    private ServerMonitorVO.JvmInfo buildJvmInfo() {
        ServerMonitorVO.JvmInfo jvm = new ServerMonitorVO.JvmInfo();
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long max = runtime.maxMemory();
        long used = Math.max(total - free, 0);
        jvm.setTotal(FileUtil.formatFileSize(total));
        jvm.setUsed(FileUtil.formatFileSize(used));
        jvm.setFree(FileUtil.formatFileSize(free));
        jvm.setMax(FileUtil.formatFileSize(max));
        jvm.setUsage(round(percent(used, max)));
        return jvm;
    }

    private ServerMonitorVO.ServerInfo buildServerInfo() {
        ServerMonitorVO.ServerInfo server = new ServerMonitorVO.ServerInfo();
        server.setOsName(System.getProperty("os.name"));
        server.setOsArch(System.getProperty("os.arch"));
        try {
            InetAddress address = InetAddress.getLocalHost();
            server.setHostName(address.getHostName());
            server.setHostIp(address.getHostAddress());
        } catch (Exception e) {
            server.setHostName("未知");
            server.setHostIp("未知");
        }
        return server;
    }

    private ServerMonitorVO.JavaInfo buildJavaInfo() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        ServerMonitorVO.JavaInfo javaInfo = new ServerMonitorVO.JavaInfo();
        javaInfo.setJavaName(runtimeBean.getVmName());
        javaInfo.setJavaVersion(System.getProperty("java.version"));
        javaInfo.setJavaHome(System.getProperty("java.home"));
        javaInfo.setProjectDir(System.getProperty("user.dir"));
        javaInfo.setInputArgs(String.join(" ", runtimeBean.getInputArguments()));

        Instant start = Instant.ofEpochMilli(runtimeBean.getStartTime());
        javaInfo.setStartTime(TIME_FORMATTER.format(start.atZone(ZoneId.systemDefault())));
        javaInfo.setRunTime(formatDuration(runtimeBean.getUptime()));
        return javaInfo;
    }

    private List<ServerMonitorVO.DiskInfo> buildDiskInfo() {
        List<ServerMonitorVO.DiskInfo> disks = new ArrayList<>();
        File[] roots = File.listRoots();
        if (roots == null) {
            return disks;
        }
        for (File root : roots) {
            try {
                FileStore store = Files.getFileStore(root.toPath());
                long total = store.getTotalSpace();
                long free = store.getUsableSpace();
                long used = Math.max(total - free, 0);

                ServerMonitorVO.DiskInfo disk = new ServerMonitorVO.DiskInfo();
                disk.setMount(root.getAbsolutePath());
                disk.setFileSystem(store.type());
                disk.setDiskType(store.name());
                disk.setTotal(FileUtil.formatFileSize(total));
                disk.setFree(FileUtil.formatFileSize(free));
                disk.setUsed(FileUtil.formatFileSize(used));
                disk.setUsage(round(percent(used, total)));
                disks.add(disk);
            } catch (Exception ignored) {
            }
        }
        return disks;
    }

    private long[] readLinuxMemInfo() {
        Path path = Path.of("/proc/meminfo");
        if (!Files.isReadable(path)) {
            return null;
        }
        long total = 0;
        long available = 0;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    total = parseMemInfoKb(line);
                } else if (line.startsWith("MemAvailable:")) {
                    available = parseMemInfoKb(line);
                }
                if (total > 0 && available > 0) {
                    break;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        if (total <= 0 || available <= 0) {
            return null;
        }
        return new long[] { total * 1024, available * 1024 };
    }

    private long parseMemInfoKb(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            return 0;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private double percent(double value) {
        if (value < 0) {
            return 0;
        }
        return value * 100.0;
    }

    private double percent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return (double) part * 100.0 / total;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String formatDuration(long uptimeMillis) {
        Duration duration = Duration.ofMillis(uptimeMillis);
        long days = duration.toDays();
        duration = duration.minusDays(days);
        long hours = duration.toHours();
        duration = duration.minusHours(hours);
        long minutes = duration.toMinutes();

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("天");
        }
        if (hours > 0 || days > 0) {
            builder.append(hours).append("小时");
        }
        builder.append(minutes).append("分钟");
        return builder.toString();
    }
}
