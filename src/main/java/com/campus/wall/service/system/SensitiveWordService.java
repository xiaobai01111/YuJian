package com.campus.wall.service.system;

import com.campus.wall.dto.system.SensitiveWordDTO;
import com.campus.wall.entity.system.SensitiveWord;

import java.util.List;

/**
 * 敏感词服务接口
 */
public interface SensitiveWordService {

    /**
     * 添加敏感词
     */
    Long addWord(SensitiveWordDTO dto);

    /**
     * 删除敏感词
     */
    void deleteWord(Long id);

    /**
     * 获取所有敏感词
     */
    List<SensitiveWord> getAllWords();

    /**
     * 检查内容是否包含敏感词
     * @return 匹配到的敏感词列表
     */
    List<SensitiveWord> checkContent(String content);

    /**
     * 过滤敏感词（替换为*）
     */
    String filterContent(String content);
}
