package com.campus.wall.service.content;

import java.util.List;
import java.util.Set;

/**
 * 敏感词过滤服务接口
 */
public interface SensitiveWordService {

    /**
     * 检测文本是否包含敏感词
     * @param text 待检测文本
     * @return 是否包含敏感词
     */
    boolean containsSensitiveWord(String text);

    /**
     * 获取文本中的敏感词列表
     * @param text 待检测文本
     * @return 敏感词列表
     */
    Set<String> findSensitiveWords(String text);

    /**
     * 替换敏感词
     * @param text 原始文本
     * @param replacement 替换字符
     * @return 过滤后的文本
     */
    String replaceSensitiveWords(String text, char replacement);

    /**
     * 添加敏感词
     * @param word 敏感词
     */
    void addSensitiveWord(String word);

    /**
     * 批量添加敏感词
     * @param words 敏感词列表
     */
    void addSensitiveWords(List<String> words);

    /**
     * 删除敏感词
     * @param word 敏感词
     */
    void removeSensitiveWord(String word);

    /**
     * 重新加载敏感词库
     */
    void reloadSensitiveWords();
}
