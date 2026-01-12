package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.ReportCreateDTO;
import com.campus.wall.dto.system.ReportHandleDTO;
import com.campus.wall.vo.system.ReportVO;

/**
 * 举报服务接口
 */
public interface ReportService {

    /**
     * 创建举报
     */
    Long createReport(ReportCreateDTO dto);

    /**
     * 处理举报
     */
    void handleReport(Long reportId, ReportHandleDTO dto);

    /**
     * 分页查询举报（管理端）
     */
    PageResult<ReportVO> queryReports(Integer status, int page, int size);

    /**
     * 获取举报详情
     */
    ReportVO getReportDetail(Long reportId);
}
