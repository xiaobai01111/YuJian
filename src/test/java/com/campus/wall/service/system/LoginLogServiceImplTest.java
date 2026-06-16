package com.campus.wall.service.system;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.campus.wall.dto.system.LoginLogQueryDTO;
import com.campus.wall.entity.system.SysLoginLog;
import com.campus.wall.mapper.system.SysLoginLogMapper;
import com.campus.wall.service.system.impl.LoginLogServiceImpl;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginLogServiceImplTest {

    @Mock
    private SysLoginLogMapper loginLogMapper;
    @Mock
    private OperLogService operLogService;

    @InjectMocks
    private LoginLogServiceImpl loginLogService;

    @Test
    void deleteLog_recordsAuditTrailWhenDeleted() {
        when(loginLogMapper.deleteById(11L)).thenReturn(1);

        loginLogService.deleteLog(11L);

        verify(loginLogMapper).deleteById(11L);
        verify(operLogService).log("audit_trail", 11L, "login_log_delete", "deleted_id=11,affected=1");
    }

    @Test
    void deleteLog_recordsAuditTrailWhenNothingDeleted() {
        when(loginLogMapper.deleteById(11L)).thenReturn(0);

        loginLogService.deleteLog(11L);

        verify(loginLogMapper).deleteById(11L);
        verify(operLogService).log("audit_trail", 11L, "login_log_delete", "deleted_id=11,affected=0");
    }

    @Test
    void clearLogs_recordsAuditTrail() {
        when(loginLogMapper.selectCount(org.mockito.ArgumentMatchers.<Wrapper<SysLoginLog>>any())).thenReturn(9L);

        loginLogService.clearLogs();

        verify(loginLogMapper).selectCount(org.mockito.ArgumentMatchers.<Wrapper<SysLoginLog>>any());
        verify(loginLogMapper).delete(org.mockito.ArgumentMatchers.<Wrapper<SysLoginLog>>any());
        verify(operLogService).log("audit_trail", null, "login_log_clear", "cleared_count=9");
    }

    @Test
    void exportLogs_escapesFormulaCells() throws IOException {
        SysLoginLog log = new SysLoginLog();
        log.setUsername("=admin");
        log.setIpaddr("+1.1.1.1");
        log.setLoginLocation("@lab");
        log.setBrowser("-chrome");
        log.setOs("=linux");
        log.setMsg("=login failed");
        log.setStatus(1);
        log.setLoginTime(LocalDateTime.now());
        when(loginLogMapper.selectList(org.mockito.ArgumentMatchers.<Wrapper<SysLoginLog>>any()))
            .thenReturn(List.of(log));

        MockHttpServletResponse response = new MockHttpServletResponse();
        loginLogService.exportLogs(new LoginLogQueryDTO(), response);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            var row = workbook.getSheetAt(0).getRow(1);
            assertThat(row).isNotNull();
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("'=admin");
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("'+1.1.1.1");
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("'@lab");
            assertThat(row.getCell(3).getStringCellValue()).isEqualTo("'-chrome");
            assertThat(row.getCell(4).getStringCellValue()).isEqualTo("'=linux");
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo("'=login failed");
        }
    }
}
