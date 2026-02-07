package com.campus.wall.controller.system;

import com.campus.wall.common.R;
import com.campus.wall.vo.system.RedisMonitorVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class RedisMonitorControllerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RedisMonitorController redisMonitorController;

    @Test
    void getRedisInfo_buildsMonitorData() {
        Properties info = new Properties();
        info.setProperty("redis_version", "7.2.0");
        info.setProperty("redis_mode", "cluster");
        info.setProperty("tcp_port", "6379");
        info.setProperty("connected_clients", "8");
        info.setProperty("uptime_in_days", "9");
        info.setProperty("used_memory_human", "128M");
        info.setProperty("maxmemory_human", "1G");
        info.setProperty("used_cpu_sys", "2.5");
        info.setProperty("used_cpu_user", "1.5");
        info.setProperty("aof_enabled", "yes");
        info.setProperty("rdb_last_bgsave_status", "ok");
        info.setProperty("total_net_input_bytes_human", "10M");
        info.setProperty("total_net_output_bytes_human", "12M");
        info.setProperty("used_memory_peak_human", "200M");
        info.setProperty("used_memory_rss_human", "180M");
        info.setProperty("used_memory_lua_human", "2M");
        info.setProperty("mem_fragmentation_ratio", "1.10");

        Properties commandStats = new Properties();
        commandStats.setProperty("cmdstat_get", "calls=20,usec=200,usec_per_call=10.0");
        commandStats.setProperty("cmdstat_set", "calls=5,usec=100,usec_per_call=20.0");
        commandStats.setProperty("foo", "bar");

        Properties keyspace = new Properties();
        keyspace.setProperty("db0", "keys=12,expires=1,avg_ttl=123");

        doReturn(info, commandStats, keyspace)
            .when(redisTemplate)
            .execute(org.mockito.Mockito.<RedisCallback<Properties>>any());

        R<RedisMonitorVO> response = redisMonitorController.getRedisInfo();

        assertThat(response.getCode()).isEqualTo(200);
        RedisMonitorVO data = response.getData();
        assertThat(data.getBasic().getVersion()).isEqualTo("7.2.0");
        assertThat(data.getBasic().getRunMode()).isEqualTo("集群");
        assertThat(data.getBasic().getKeyCount()).isEqualTo("12");
        assertThat(data.getBasic().getUsedCpu()).isEqualTo("4.00");
        assertThat(data.getBasic().getAofEnabled()).isEqualTo("是");
        assertThat(data.getMemory().getUsed()).isEqualTo("128M");
        assertThat(data.getCommandStats()).hasSize(2);
        assertThat(data.getCommandStats().get(0).getCommand()).isEqualTo("get");
        assertThat(data.getCommandStats().get(0).getCalls()).isEqualTo(20L);
    }

    @Test
    void getRedisInfo_handlesNullSectionsAndInvalidNumbers() {
        Properties info = new Properties();
        info.setProperty("cluster_enabled", "0");
        info.setProperty("used_cpu_sys", "bad");
        info.setProperty("used_cpu_user", "3.0");

        doReturn(info, null, new Properties())
            .when(redisTemplate)
            .execute(org.mockito.Mockito.<RedisCallback<Properties>>any());

        R<RedisMonitorVO> response = redisMonitorController.getRedisInfo();

        assertThat(response.getCode()).isEqualTo(200);
        RedisMonitorVO data = response.getData();
        assertThat(data.getBasic().getRunMode()).isEqualTo("单机");
        assertThat(data.getBasic().getKeyCount()).isEqualTo("-");
        assertThat(data.getBasic().getUsedCpu()).isEqualTo("0.00");
        assertThat(data.getCommandStats()).isEmpty();
    }
}
