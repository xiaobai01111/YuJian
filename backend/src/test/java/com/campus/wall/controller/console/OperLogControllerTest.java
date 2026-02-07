package com.campus.wall.controller.console;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.OperLogQueryDTO;
import com.campus.wall.entity.system.SysOperLog;
import com.campus.wall.mapper.system.SysOperLogMapper;
import com.campus.wall.vo.system.OperLogVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperLogControllerTest {

    @Mock
    private SysOperLogMapper operLogMapper;

    @InjectMocks
    private OperLogController operLogController;

    @Test
    void list_mapsPageRecords() {
        OperLogQueryDTO query = new OperLogQueryDTO();
        query.setPage(1);
        query.setSize(10);

        SysOperLog log = new SysOperLog();
        log.setId(1L);
        log.setOperatorName("admin");
        log.setAction("DELETE");

        Page<SysOperLog> page = new Page<>(1, 10);
        page.setRecords(List.of(log));
        page.setTotal(1);

        when(operLogMapper.selectPage(org.mockito.Mockito.<Page<SysOperLog>>any(),
            org.mockito.Mockito.<LambdaQueryWrapper<SysOperLog>>any())).thenReturn(page);

        R<com.campus.wall.common.PageResult<OperLogVO>> response = operLogController.list(query);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getRecords()).hasSize(1);
        assertThat(response.getData().getRecords().get(0).getOperatorName()).isEqualTo("admin");
    }

    @Test
    void list_invalidStartTime_throws() {
        OperLogQueryDTO query = new OperLogQueryDTO();
        query.setStartTime("2026/02/01");

        assertThatThrownBy(() -> operLogController.list(query))
            .isInstanceOf(BusinessException.class)
            .hasMessage("开始时间格式错误");
    }

    @Test
    void list_invalidEndTime_throws() {
        OperLogQueryDTO query = new OperLogQueryDTO();
        query.setEndTime("2026-02-31");

        assertThatThrownBy(() -> operLogController.list(query))
            .isInstanceOf(BusinessException.class)
            .hasMessage("结束时间格式错误");
    }

    @Test
    void delete_and_clear_delegateToMapper() {
        operLogController.delete(5L);
        operLogController.clear();

        verify(operLogMapper).deleteById(5L);
        verify(operLogMapper).delete(org.mockito.Mockito.<LambdaQueryWrapper<SysOperLog>>any());
    }

    @Test
    void export_writesExcelResponse() {
        OperLogQueryDTO query = new OperLogQueryDTO();

        SysOperLog log = new SysOperLog();
        log.setOperatorName("admin");
        log.setTargetType("POST");
        log.setTargetId(101L);
        log.setAction("UPDATE");
        log.setReason("test");
        log.setIpAddress("127.0.0.1");
        log.setCreatedAt(LocalDateTime.now());
        when(operLogMapper.selectList(org.mockito.Mockito.<LambdaQueryWrapper<SysOperLog>>any()))
            .thenReturn(List.of(log));

        MockHttpServletResponse response = new MockHttpServletResponse();
        operLogController.export(query, response);

        assertThat(response.getContentType())
            .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        assertThat(response.getHeader("Content-Disposition")).contains("attachment");
        assertThat(response.getContentAsByteArray().length).isGreaterThan(0);
    }

    @Test
    void export_invalidDate_throws() {
        OperLogQueryDTO query = new OperLogQueryDTO();
        query.setEndTime("bad-date");

        assertThatThrownBy(() -> operLogController.export(query, new MockHttpServletResponse()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("结束时间格式错误");
    }
}
