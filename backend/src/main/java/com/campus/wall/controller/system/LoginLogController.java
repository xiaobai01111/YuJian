package com.campus.wall.controller.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.LoginLogQueryDTO;
import com.campus.wall.service.system.LoginLogService;
import com.campus.wall.vo.system.LoginLogVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/console/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @GetMapping
    public R<PageResult<LoginLogVO>> list(@Validated LoginLogQueryDTO query) {
        return R.ok(loginLogService.queryLogs(query));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        loginLogService.deleteLog(id);
        return R.ok();
    }

    @DeleteMapping("/clear")
    public R<Void> clear() {
        loginLogService.clearLogs();
        return R.ok();
    }

    @GetMapping("/export")
    public void export(@Validated LoginLogQueryDTO query, HttpServletResponse response) {
        loginLogService.exportLogs(query, response);
    }
}
