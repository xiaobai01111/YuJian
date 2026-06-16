package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.LoginLogQueryDTO;
import com.campus.wall.vo.system.LoginLogVO;
import jakarta.servlet.http.HttpServletResponse;

public interface LoginLogService {

    PageResult<LoginLogVO> queryLogs(LoginLogQueryDTO query);

    void deleteLog(Long id);

    void clearLogs();

    void exportLogs(LoginLogQueryDTO query, HttpServletResponse response);

    void recordLogin(Long userId, String username, Integer status, String msg, String ipaddr, String userAgent);
}
