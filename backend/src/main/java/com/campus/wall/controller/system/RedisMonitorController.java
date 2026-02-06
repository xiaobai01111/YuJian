package com.campus.wall.controller.system;

import com.campus.wall.common.R;
import com.campus.wall.util.FileUtil;
import com.campus.wall.vo.system.RedisMonitorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Redis监控控制器
 */
@RestController
@RequestMapping("/api/v1/console/monitor/redis")
@RequiredArgsConstructor
public class RedisMonitorController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping
    public R<RedisMonitorVO> getRedisInfo() {
        Map<String, String> info = fetchInfo();
        Map<String, String> commandStats = fetchInfoSection("commandstats");
        Map<String, String> keyspace = fetchInfoSection("keyspace");

        RedisMonitorVO vo = new RedisMonitorVO();
        vo.setBasic(buildBasicInfo(info, keyspace));
        vo.setMemory(buildMemoryInfo(info));
        vo.setCommandStats(buildCommandStats(commandStats));
        return R.ok(vo);
    }

    private Map<String, String> fetchInfo() {
        Properties props = redisTemplate.execute((RedisCallback<Properties>) connection -> connection.serverCommands().info());
        return toMap(props);
    }

    private Map<String, String> fetchInfoSection(String section) {
        Properties props = redisTemplate.execute(
            (RedisCallback<Properties>) connection -> connection.serverCommands().info(section)
        );
        return toMap(props);
    }

    private Map<String, String> toMap(Properties props) {
        Map<String, String> map = new HashMap<>();
        if (props == null) {
            return map;
        }
        for (String name : props.stringPropertyNames()) {
            map.put(name, props.getProperty(name));
        }
        return map;
    }

    private RedisMonitorVO.BasicInfo buildBasicInfo(Map<String, String> info, Map<String, String> keyspace) {
        RedisMonitorVO.BasicInfo basic = new RedisMonitorVO.BasicInfo();
        basic.setVersion(info.getOrDefault("redis_version", "-"));
        basic.setRunMode(formatRunMode(info));
        basic.setPort(info.getOrDefault("tcp_port", "-"));
        basic.setConnectedClients(info.getOrDefault("connected_clients", "-"));
        basic.setUptimeDays(info.getOrDefault("uptime_in_days", "-"));
        basic.setUsedMemory(formatBytes(info.get("used_memory"), info.get("used_memory_human")));
        basic.setUsedCpu(formatDouble(sumDouble(info.get("used_cpu_sys"), info.get("used_cpu_user"))));
        basic.setMaxMemory(formatBytes(info.get("maxmemory"), info.get("maxmemory_human")));
        basic.setAofEnabled("yes".equalsIgnoreCase(info.get("aof_enabled")) ? "是" : "否");
        basic.setRdbStatus(info.getOrDefault("rdb_last_bgsave_status", "-"));
        basic.setKeyCount(parseKeyCount(keyspace));
        basic.setNetworkInput(formatBytes(info.get("total_net_input_bytes"), info.get("total_net_input_bytes_human")));
        basic.setNetworkOutput(formatBytes(info.get("total_net_output_bytes"), info.get("total_net_output_bytes_human")));
        return basic;
    }

    private RedisMonitorVO.MemoryInfo buildMemoryInfo(Map<String, String> info) {
        RedisMonitorVO.MemoryInfo memory = new RedisMonitorVO.MemoryInfo();
        memory.setUsed(formatBytes(info.get("used_memory"), info.get("used_memory_human")));
        memory.setUsedPeak(formatBytes(info.get("used_memory_peak"), info.get("used_memory_peak_human")));
        memory.setUsedRss(formatBytes(info.get("used_memory_rss"), info.get("used_memory_rss_human")));
        memory.setUsedLua(formatBytes(info.get("used_memory_lua"), info.get("used_memory_lua_human")));
        memory.setFragmentationRatio(info.getOrDefault("mem_fragmentation_ratio", "-"));
        memory.setMax(formatBytes(info.get("maxmemory"), info.get("maxmemory_human")));
        return memory;
    }

    private List<RedisMonitorVO.CommandStat> buildCommandStats(Map<String, String> stats) {
        List<RedisMonitorVO.CommandStat> list = new ArrayList<>();
        if (stats == null || stats.isEmpty()) {
            return list;
        }
        for (Map.Entry<String, String> entry : stats.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("cmdstat_")) {
                continue;
            }
            String command = key.substring(8);
            String value = entry.getValue();
            Map<String, String> parts = parseKeyValue(value);
            RedisMonitorVO.CommandStat stat = new RedisMonitorVO.CommandStat();
            stat.setCommand(command);
            stat.setCalls(parseLong(parts.get("calls")));
            stat.setUsec(parseLong(parts.get("usec")));
            stat.setUsecPerCall(parseDouble(parts.get("usec_per_call")));
            list.add(stat);
        }
        list.sort(Comparator.comparing(RedisMonitorVO.CommandStat::getCalls, Comparator.nullsLast(Long::compareTo)).reversed());
        return list;
    }

    private Map<String, String> parseKeyValue(String value) {
        Map<String, String> map = new HashMap<>();
        if (value == null || value.isEmpty()) {
            return map;
        }
        String[] parts = value.split(",");
        for (String part : parts) {
            String[] pair = part.split("=");
            if (pair.length == 2) {
                map.put(pair[0], pair[1]);
            }
        }
        return map;
    }

    private String parseKeyCount(Map<String, String> keyspace) {
        if (keyspace == null || keyspace.isEmpty()) {
            return "-";
        }
        String db0 = keyspace.get("db0");
        if (db0 == null) {
            return "-";
        }
        Map<String, String> parts = parseKeyValue(db0);
        return parts.getOrDefault("keys", "-");
    }

    private String formatBytes(String bytes, String human) {
        if (human != null && !human.isBlank()) {
            return human;
        }
        if (bytes == null || bytes.isBlank()) {
            return "-";
        }
        return FileUtil.formatFileSize(parseLong(bytes));
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double sumDouble(String a, String b) {
        double first = 0;
        double second = 0;
        try {
            if (a != null) {
                first = Double.parseDouble(a);
            }
            if (b != null) {
                second = Double.parseDouble(b);
            }
        } catch (NumberFormatException ignored) {
        }
        return first + second;
    }

    private String formatDouble(double value) {
        return String.format("%.2f", value);
    }

    private String formatRunMode(Map<String, String> info) {
        String mode = info.get("redis_mode");
        if (mode == null || mode.isBlank()) {
            String clusterEnabled = info.get("cluster_enabled");
            if ("1".equals(clusterEnabled)) {
                mode = "cluster";
            } else if ("0".equals(clusterEnabled)) {
                mode = "standalone";
            }
        }
        if (mode == null || mode.isBlank()) {
            return "-";
        }
        return switch (mode.toLowerCase()) {
            case "standalone" -> "单机";
            case "cluster" -> "集群";
            case "sentinel" -> "哨兵";
            default -> mode;
        };
    }
}
