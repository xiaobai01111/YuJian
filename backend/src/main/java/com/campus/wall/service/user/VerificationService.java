package com.campus.wall.service.user;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.user.VerificationHandleDTO;
import com.campus.wall.vo.user.VerificationVO;

/**
 * 身份审核服务接口
 */
public interface VerificationService {

    /**
     * 分页查询审核记录
     */
    PageResult<VerificationVO> queryVerifications(Integer status, int page, int size);

    /**
     * 获取审核详情
     */
    VerificationVO getVerificationDetail(Long id);

    /**
     * 处理审核
     */
    void handleVerification(Long id, VerificationHandleDTO dto);
}
