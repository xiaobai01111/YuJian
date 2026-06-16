package com.campus.wall.service.system.impl;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.LoginLogQueryDTO;
import com.campus.wall.entity.system.SysLoginLog;
import com.campus.wall.mapper.system.SysLoginLogMapper;
import com.campus.wall.service.system.LoginLogService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.util.ExcelSecurityUtil;
import com.campus.wall.vo.system.LoginLogVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private static final String AUDIT_TRAIL_TARGET_TYPE = "audit_trail";

    private final SysLoginLogMapper loginLogMapper;
    private final OperLogService operLogService;

    @Override
    public PageResult<LoginLogVO> queryLogs(LoginLogQueryDTO query) {
        LambdaQueryWrapper<SysLoginLog> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(SysLoginLog::getLoginTime);

        Page<SysLoginLog> page = loginLogMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()), wrapper
        );

        List<LoginLogVO> records = page.getRecords().stream()
            .map(this::toVO)
            .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    public void deleteLog(Long id) {
        int deleted = loginLogMapper.deleteById(id);
        operLogService.log(AUDIT_TRAIL_TARGET_TYPE, id, "login_log_delete", "deleted_id=" + id + ",affected=" + deleted);
    }

    @Override
    public void clearLogs() {
        Long clearedCount = loginLogMapper.selectCount(new LambdaQueryWrapper<>());
        loginLogMapper.delete(new LambdaQueryWrapper<>());
        operLogService.log(
            AUDIT_TRAIL_TARGET_TYPE,
            null,
            "login_log_clear",
            "cleared_count=" + (clearedCount == null ? 0 : clearedCount)
        );
    }

    @Override
    public void exportLogs(LoginLogQueryDTO query, HttpServletResponse response) {
        LambdaQueryWrapper<SysLoginLog> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(SysLoginLog::getLoginTime);
        List<SysLoginLog> logs = loginLogMapper.selectList(wrapper);

        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("username", "用户名");
        writer.addHeaderAlias("ipaddr", "登录IP");
        writer.addHeaderAlias("loginLocation", "登录地点");
        writer.addHeaderAlias("browser", "浏览器");
        writer.addHeaderAlias("os", "操作系统");
        writer.addHeaderAlias("statusText", "状态");
        writer.addHeaderAlias("msg", "提示信息");
        writer.addHeaderAlias("loginTime", "登录时间");

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysLoginLog log : logs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("username", ExcelSecurityUtil.escapeFormula(log.getUsername()));
            row.put("ipaddr", ExcelSecurityUtil.escapeFormula(log.getIpaddr()));
            row.put("loginLocation", ExcelSecurityUtil.escapeFormula(log.getLoginLocation()));
            row.put("browser", ExcelSecurityUtil.escapeFormula(log.getBrowser()));
            row.put("os", ExcelSecurityUtil.escapeFormula(log.getOs()));
            row.put("statusText", log.getStatus() != null && log.getStatus() == 0 ? "成功" : "失败");
            row.put("msg", ExcelSecurityUtil.escapeFormula(log.getMsg()));
            row.put("loginTime", log.getLoginTime());
            rows.add(row);
        }
        writer.write(rows, true);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        try {
            response.setHeader("Content-Disposition", com.campus.wall.util.HttpHeaderUtil.buildContentDisposition("登录日志.xlsx", true));
            writer.flush(response.getOutputStream(), true);
            writer.close();
        } catch (IOException e) {
            throw new BusinessException("导出失败");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogin(Long userId, String username, Integer status, String msg, String ipaddr, String userAgent) {
        try {
            SysLoginLog logEntity = new SysLoginLog();
            logEntity.setUserId(userId);
            logEntity.setUsername(username);
            logEntity.setIpaddr(ipaddr);
            logEntity.setUserAgent(userAgent);
            logEntity.setStatus(status);
            logEntity.setMsg(msg);
            logEntity.setLoginTime(LocalDateTime.now());

            logEntity.setLoginLocation(resolveLocation(ipaddr));

            if (StringUtils.hasText(userAgent)) {
                UserAgent ua = UserAgentUtil.parse(userAgent);
                if (ua != null) {
                    logEntity.setBrowser(ua.getBrowser().getName());
                    logEntity.setOs(ua.getOs().getName());
                }
            }

            loginLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    private LoginLogVO toVO(SysLoginLog entity) {
        LoginLogVO vo = new LoginLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private LambdaQueryWrapper<SysLoginLog> buildQueryWrapper(LoginLogQueryDTO query) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysLoginLog::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getIpaddr())) {
            wrapper.like(SysLoginLog::getIpaddr, query.getIpaddr());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysLoginLog::getStatus, query.getStatus());
        }
        applyLoginTimeRange(wrapper, query.getLoginTimeStart(), query.getLoginTimeEnd());
        return wrapper;
    }

    private void applyLoginTimeRange(LambdaQueryWrapper<SysLoginLog> wrapper, String start, String end) {
        try {
            if (StringUtils.hasText(start)) {
                LocalDate startDate = LocalDate.parse(start);
                wrapper.ge(SysLoginLog::getLoginTime, startDate.atStartOfDay());
            }
            if (StringUtils.hasText(end)) {
                LocalDate endDate = LocalDate.parse(end);
                wrapper.le(SysLoginLog::getLoginTime, endDate.plusDays(1).atStartOfDay().minusNanos(1));
            }
        } catch (Exception e) {
            log.warn("登录时间范围解析失败 start={} end={}", start, end);
        }
    }

    private String resolveLocation(String ipaddr) {
        if (!StringUtils.hasText(ipaddr)) {
            return "未知";
        }
        if (isPrivateIp(ipaddr)) {
            return "内网IP";
        }
        return "公网IP";
    }

    private boolean isPrivateIp(String ipaddr) {
        try {
            InetAddress address = InetAddress.getByName(ipaddr);
            return address.isSiteLocalAddress()
                || address.isLoopbackAddress()
                || address.isAnyLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
