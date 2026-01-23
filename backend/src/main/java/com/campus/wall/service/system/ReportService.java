package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.ReportBatchHandleDTO;
import com.campus.wall.dto.system.ReportBatchCreateDTO;
import com.campus.wall.dto.system.ReportCreateDTO;
import com.campus.wall.dto.system.ReportHandleDTO;
import com.campus.wall.vo.common.BatchActionResultVO;
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
     * 批量创建举报
     */
    BatchActionResultVO createReports(ReportBatchCreateDTO dto);

    /**
     * 处理举报
     */
    void handleReport(Long reportId, ReportHandleDTO dto);

    /**
     * 批量处理举报
     */
    void handleReports(ReportBatchHandleDTO dto);

    /**
     * 软删除举报（回收站）
     */
    void deleteReportByAdmin(Long reportId, String reason);

    /**
     * 恢复举报
     */
    void restoreReportByAdmin(Long reportId, String reason);

    /**
     * 彻底删除举报
     */
    void purgeReportByAdmin(Long reportId, String reason);

    /**
     * 分页查询举报（管理端）
     */
    PageResult<ReportVO> queryReports(Integer status, int page, int size);

    /**
     * 回收站举报列表
     */
    PageResult<ReportVO> queryDeletedReports(Integer status, int page, int size);

    /**
     * 获取举报详情
     */
    ReportVO getReportDetail(Long reportId);
}
