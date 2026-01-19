package com.campus.wall.controller.console;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.ReportBatchHandleDTO;
import com.campus.wall.dto.system.ReportHandleDTO;
import com.campus.wall.service.system.ReportService;
import com.campus.wall.vo.system.ReportVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/console/reports")
@RequiredArgsConstructor
public class ReportConsoleController {

    private final ReportService reportService;

    @SaCheckPermission("content:report:list")
    @GetMapping
    public R<PageResult<ReportVO>> list(@RequestParam(required = false) Integer status,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return R.ok(reportService.queryReports(status, page, size));
    }

    @SaCheckPermission("content:report:list")
    @GetMapping("/{id}")
    public R<ReportVO> detail(@PathVariable Long id) {
        return R.ok(reportService.getReportDetail(id));
    }

    @SaCheckPermission("content:report:handle")
    @PutMapping("/{id}/handle")
    public R<Void> handle(@PathVariable Long id, @RequestBody @Valid ReportHandleDTO dto) {
        reportService.handleReport(id, dto);
        return R.ok();
    }

    @SaCheckPermission("content:report:batch-handle")
    @PostMapping("/batch-handle")
    public R<Void> batchHandle(@RequestBody @Valid ReportBatchHandleDTO dto) {
        reportService.handleReports(dto);
        return R.ok();
    }

    @SaCheckPermission("content:report:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id,
                          @RequestParam(value = "reason", required = false) String reason) {
        reportService.deleteReportByAdmin(id, reason);
        return R.ok();
    }
}
