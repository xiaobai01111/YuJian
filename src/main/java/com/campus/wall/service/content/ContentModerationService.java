package com.campus.wall.service.content;

import org.springframework.web.multipart.MultipartFile;

/**
 * 内容安全审核服务接口
 */
public interface ContentModerationService {

    /**
     * 审核图片内容
     * @param imageUrl 图片URL
     * @return 是否通过审核
     */
    boolean moderateImage(String imageUrl);

    /**
     * 审核文本内容
     * @param text 文本内容
     * @return 是否通过审核
     */
    boolean moderateText(String text);

    /**
     * 病毒扫描
     * @param file 待扫描文件
     * @return 是否安全
     */
    boolean scanFile(MultipartFile file);

    /**
     * 异步审核图片
     * @param fileId 文件ID
     * @param imageUrl 图片URL
     */
    void asyncModerateImage(Long fileId, String imageUrl);
}
