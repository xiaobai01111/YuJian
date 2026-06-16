package com.campus.wall.controller.console;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.OperLogQueryDTO;
import com.campus.wall.entity.system.SysOperLog;
import com.campus.wall.mapper.system.SysOperLogMapper;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.util.ExcelSecurityUtil;
import com.campus.wall.vo.system.OperLogVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;

@RestController
@RequestMapping("/api/v1/console/oper-logs")
@RequiredArgsConstructor
public class OperLogController {

    private static final String AUDIT_TRAIL_TARGET_TYPE = "audit_trail";

    private final SysOperLogMapper operLogMapper;
    private final OperLogService operLogService;

    @GetMapping
    public R<PageResult<OperLogVO>> list(@jakarta.validation.Valid OperLogQueryDTO query) {
        LambdaQueryWrapper<SysOperLog> wrapper = buildWrapper(query);
        wrapper.orderByDesc(SysOperLog::getCreatedAt);
        Page<SysOperLog> page = operLogMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        List<OperLogVO> records = page.getRecords().stream()
            .map(this::toVO)
            .collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent()));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        SysOperLog existing = operLogMapper.selectById(id);
        if (existing != null && AUDIT_TRAIL_TARGET_TYPE.equalsIgnoreCase(trimToEmpty(existing.getTargetType()))) {
            operLogService.log(AUDIT_TRAIL_TARGET_TYPE, id, "oper_log_delete_blocked", "blocked_target=audit_trail");
            throw new BusinessException(ResultCode.FORBIDDEN, "审计留痕不可删除");
        }
        int deleted = operLogMapper.deleteById(id);
        operLogService.log(AUDIT_TRAIL_TARGET_TYPE, id, "oper_log_delete", "deleted_id=" + id + ",affected=" + deleted);
        return R.ok();
    }

    @DeleteMapping("/clear")
    public R<Void> clear() {
        LambdaQueryWrapper<SysOperLog> clearWrapper = buildClearWrapper();
        Long clearedCount = operLogMapper.selectCount(clearWrapper);
        operLogMapper.delete(clearWrapper);
        operLogService.log(
            AUDIT_TRAIL_TARGET_TYPE,
            null,
            "oper_log_clear",
            "cleared_count=" + (clearedCount == null ? 0 : clearedCount)
        );
        return R.ok();
    }

    @GetMapping("/export")
    public void export(OperLogQueryDTO query, HttpServletResponse response) {
        LambdaQueryWrapper<SysOperLog> wrapper = buildWrapper(query);
        wrapper.orderByDesc(SysOperLog::getCreatedAt);
        List<SysOperLog> logs = operLogMapper.selectList(wrapper);

        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("operatorName", "操作人");
        writer.addHeaderAlias("targetType", "目标类型");
        writer.addHeaderAlias("targetId", "目标ID");
        writer.addHeaderAlias("action", "动作");
        writer.addHeaderAlias("reason", "原因");
        writer.addHeaderAlias("ipAddress", "IP地址");
        writer.addHeaderAlias("userAgent", "用户代理");
        writer.addHeaderAlias("requestBodyDigest", "请求体摘要");
        writer.addHeaderAlias("createdAt", "操作时间");

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysOperLog log : logs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("operatorName", ExcelSecurityUtil.escapeFormula(log.getOperatorName()));
            row.put("targetType", ExcelSecurityUtil.escapeFormula(log.getTargetType()));
            row.put("targetId", log.getTargetId());
            row.put("action", ExcelSecurityUtil.escapeFormula(log.getAction()));
            row.put("reason", ExcelSecurityUtil.escapeFormula(log.getReason()));
            row.put("ipAddress", ExcelSecurityUtil.escapeFormula(log.getIpAddress()));
            row.put("userAgent", ExcelSecurityUtil.escapeFormula(log.getUserAgent()));
            row.put("requestBodyDigest", ExcelSecurityUtil.escapeFormula(log.getRequestBodyDigest()));
            row.put("createdAt", log.getCreatedAt());
            rows.add(row);
        }
        writer.write(rows, true);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        try {
            response.setHeader("Content-Disposition", com.campus.wall.util.HttpHeaderUtil.buildContentDisposition("操作日志.xlsx", true));
            writer.flush(response.getOutputStream(), true);
            writer.close();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "导出失败");
        }
    }

    private OperLogVO toVO(SysOperLog log) {
        OperLogVO vo = new OperLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }

    private LambdaQueryWrapper<SysOperLog> buildWrapper(OperLogQueryDTO query) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (query == null) {
            return wrapper;
        }
        if (StringUtils.hasText(query.getOperatorName())) {
            wrapper.like(SysOperLog::getOperatorName, query.getOperatorName());
        }
        if (StringUtils.hasText(query.getTargetType())) {
            wrapper.eq(SysOperLog::getTargetType, query.getTargetType());
        }
        if (StringUtils.hasText(query.getAction())) {
            wrapper.eq(SysOperLog::getAction, query.getAction());
        }
        if (StringUtils.hasText(query.getStartTime())) {
            LocalDate start = parseDateOrThrow(query.getStartTime(), "开始时间");
            wrapper.ge(SysOperLog::getCreatedAt, start.atStartOfDay());
        }
        if (StringUtils.hasText(query.getEndTime())) {
            LocalDate end = parseDateOrThrow(query.getEndTime(), "结束时间");
            wrapper.le(SysOperLog::getCreatedAt, end.plusDays(1).atStartOfDay().minusNanos(1));
        }
        return wrapper;
    }

    private LocalDate parseDateOrThrow(String value, String label) {
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "格式错误");
        }
    }

    private LambdaQueryWrapper<SysOperLog> buildClearWrapper() {
        return new LambdaQueryWrapper<SysOperLog>()
            .and(w -> w.isNull(SysOperLog::getTargetType)
                .or()
                .ne(SysOperLog::getTargetType, AUDIT_TRAIL_TARGET_TYPE));
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
