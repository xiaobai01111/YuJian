package com.campus.wall.service.content.impl;

import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.service.content.ContentModerationService;
import com.campus.wall.service.content.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 内容安全审核服务实现
 * 
 * 注意：生产环境应接入阿里云/腾讯云内容安全API
 * 当前为模拟实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationServiceImpl implements ContentModerationService {

    private final SensitiveWordService sensitiveWordService;
    private final FileRecordMapper fileRecordMapper;

    // 审核状态：0待审核 1审核通过 2审核不通过
    private static final int AUDIT_PENDING = 0;
    private static final int AUDIT_PASSED = 1;
    private static final int AUDIT_REJECTED = 2;

    @Override
    public boolean moderateImage(String imageUrl) {
        // TODO: 接入阿里云/腾讯云图片审核API
        // 当前模拟实现：默认通过
        log.info("图片审核: {}", imageUrl);
        return true;
    }

    @Override
    public boolean moderateText(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        // 使用敏感词服务检测
        return !sensitiveWordService.containsSensitiveWord(text);
    }

    @Override
    @Async
    public void asyncModerateImage(Long fileId, String imageUrl) {
        try {
            boolean passed = moderateImage(imageUrl);
            int auditStatus = passed ? AUDIT_PASSED : AUDIT_REJECTED;
            fileRecordMapper.updateAuditStatus(fileId, auditStatus);
            
            if (!passed) {
                log.warn("图片审核不通过，已标记: fileId={}, url={}", fileId, imageUrl);
                // TODO: 发送通知给用户
            }
        } catch (Exception e) {
            log.error("异步图片审核失败: fileId={}", fileId, e);
        }
    }
}
