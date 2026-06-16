package com.campus.wall.controller.console;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.R;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.OperLogQueryDTO;
import com.campus.wall.entity.system.SysOperLog;
import com.campus.wall.mapper.system.SysOperLogMapper;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.vo.system.OperLogVO;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperLogControllerTest {

    @Mock
    private SysOperLogMapper operLogMapper;
    @Mock
    private OperLogService operLogService;

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
        when(operLogMapper.deleteById(5L)).thenReturn(1);
        when(operLogMapper.selectCount(org.mockito.ArgumentMatchers.<Wrapper<SysOperLog>>any())).thenReturn(3L);

        operLogController.delete(5L);
        operLogController.clear();

        verify(operLogMapper).deleteById(5L);
        verify(operLogMapper).selectCount(org.mockito.ArgumentMatchers.<Wrapper<SysOperLog>>any());
        verify(operLogMapper).delete(org.mockito.Mockito.<LambdaQueryWrapper<SysOperLog>>any());
        verify(operLogService).log("audit_trail", 5L, "oper_log_delete", "deleted_id=5,affected=1");
        verify(operLogService).log("audit_trail", null, "oper_log_clear", "cleared_count=3");
    }

    @Test
    void delete_auditTrail_forbidden() {
        SysOperLog auditTrail = new SysOperLog();
        auditTrail.setId(8L);
        auditTrail.setTargetType("audit_trail");
        when(operLogMapper.selectById(8L)).thenReturn(auditTrail);

        assertThatThrownBy(() -> operLogController.delete(8L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode())
            .hasMessage("审计留痕不可删除");

        verify(operLogMapper, never()).deleteById(8L);
        verify(operLogService).log("audit_trail", 8L, "oper_log_delete_blocked", "blocked_target=audit_trail");
        verify(operLogService, never()).log(eq("audit_trail"), eq(8L), eq("oper_log_delete"), any());
        verify(operLogService, never()).log(eq("audit_trail"), isNull(), eq("oper_log_clear"), any());
    }

    @Test
    void export_writesExcelResponse() throws IOException {
        OperLogQueryDTO query = new OperLogQueryDTO();

        SysOperLog log = new SysOperLog();
        log.setOperatorName("=admin");
        log.setTargetType("@POST");
        log.setTargetId(101L);
        log.setAction("+UPDATE");
        log.setReason("-test");
        log.setIpAddress("=127.0.0.1");
        log.setUserAgent("=ua");
        log.setRequestBodyDigest("=digest");
        log.setCreatedAt(LocalDateTime.now());
        when(operLogMapper.selectList(org.mockito.Mockito.<LambdaQueryWrapper<SysOperLog>>any()))
            .thenReturn(List.of(log));

        MockHttpServletResponse response = new MockHttpServletResponse();
        operLogController.export(query, response);

        assertThat(response.getContentType())
            .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        assertThat(response.getHeader("Content-Disposition")).contains("attachment");
        assertThat(response.getContentAsByteArray().length).isGreaterThan(0);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            var row = workbook.getSheetAt(0).getRow(1);
            assertThat(row).isNotNull();
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("'=admin");
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("'@POST");
            assertThat(row.getCell(3).getStringCellValue()).isEqualTo("'+UPDATE");
            assertThat(row.getCell(4).getStringCellValue()).isEqualTo("'-test");
            assertThat(row.getCell(5).getStringCellValue()).isEqualTo("'=127.0.0.1");
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo("'=ua");
            assertThat(row.getCell(7).getStringCellValue()).isEqualTo("'=digest");
        }
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
