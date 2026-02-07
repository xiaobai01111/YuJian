package com.campus.wall.controller.system;

import com.campus.wall.common.R;
import com.campus.wall.vo.system.ServerMonitorVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServerMonitorControllerTest {

    @Test
    void getServerInfo_returnsAllSections() {
        ServerMonitorController controller = new ServerMonitorController();

        R<ServerMonitorVO> response = controller.getServerInfo();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getCpu()).isNotNull();
        assertThat(response.getData().getMemory()).isNotNull();
        assertThat(response.getData().getJvm()).isNotNull();
        assertThat(response.getData().getServer()).isNotNull();
        assertThat(response.getData().getJavaInfo()).isNotNull();
        assertThat(response.getData().getDisks()).isNotNull();
        assertThat(response.getData().getCpu().getCoreCount()).isGreaterThan(0);
    }
}
