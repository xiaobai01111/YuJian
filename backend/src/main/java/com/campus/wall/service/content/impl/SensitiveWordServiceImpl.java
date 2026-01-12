package com.campus.wall.service.content.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.entity.system.SensitiveWord;
import com.campus.wall.mapper.system.SensitiveWordMapper;
import com.campus.wall.service.content.SensitiveWordService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 敏感词过滤服务实现
 * 使用 DFA (确定有限状态自动机) 算法实现高效匹配
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;

    // DFA 状态机
    private volatile Map<Character, Object> sensitiveWordMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        reloadSensitiveWords();
    }

    @Override
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            int length = checkSensitiveWord(text, i);
            if (length > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> findSensitiveWords(String text) {
        Set<String> sensitiveWords = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return sensitiveWords;
        }
        for (int i = 0; i < text.length(); i++) {
            int length = checkSensitiveWord(text, i);
            if (length > 0) {
                sensitiveWords.add(text.substring(i, i + length));
                i = i + length - 1;
            }
        }
        return sensitiveWords;
    }

    @Override
    public String replaceSensitiveWords(String text, char replacement) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder(text);
        for (int i = 0; i < result.length(); i++) {
            int length = checkSensitiveWord(result.toString(), i);
            if (length > 0) {
                for (int j = i; j < i + length; j++) {
                    result.setCharAt(j, replacement);
                }
                i = i + length - 1;
            }
        }
        return result.toString();
    }

    @Override
    public void addSensitiveWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }
        // 保存到数据库
        SensitiveWord entity = new SensitiveWord();
        entity.setWord(word.trim());
        entity.setLevel(0);
        sensitiveWordMapper.insert(entity);
        
        // 更新内存
        addWordToMap(word.trim());
    }

    @Override
    public void addSensitiveWords(List<String> words) {
        if (words == null || words.isEmpty()) {
            return;
        }
        for (String word : words) {
            addSensitiveWord(word);
        }
    }

    @Override
    public void removeSensitiveWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }
        sensitiveWordMapper.delete(
                new LambdaQueryWrapper<SensitiveWord>()
                        .eq(SensitiveWord::getWord, word.trim())
        );
        // 重新加载
        reloadSensitiveWords();
    }

    @Override
    public void reloadSensitiveWords() {
        List<SensitiveWord> words = sensitiveWordMapper.selectList(
                new LambdaQueryWrapper<SensitiveWord>()
                        .ge(SensitiveWord::getLevel, 0)
        );
        
        Map<Character, Object> newMap = new ConcurrentHashMap<>();
        for (SensitiveWord word : words) {
            addWordToMap(word.getWord(), newMap);
        }
        sensitiveWordMap = newMap;
        log.info("敏感词库已加载，共 {} 个敏感词", words.size());
    }

    /**
     * 检查敏感词，返回敏感词长度，0表示不是敏感词
     */
    @SuppressWarnings("unchecked")
    private int checkSensitiveWord(String text, int beginIndex) {
        Map<Character, Object> currentMap = sensitiveWordMap;
        int matchLength = 0;
        int tempLength = 0;

        for (int i = beginIndex; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            Map<Character, Object> nextMap = (Map<Character, Object>) currentMap.get(c);

            if (nextMap == null) {
                break;
            }

            tempLength++;
            if (Boolean.TRUE.equals(nextMap.get('$'))) {
                matchLength = tempLength;
            }
            currentMap = nextMap;
        }

        return matchLength;
    }

    private void addWordToMap(String word) {
        addWordToMap(word, sensitiveWordMap);
    }

    @SuppressWarnings("unchecked")
    private void addWordToMap(String word, Map<Character, Object> map) {
        if (word == null || word.isEmpty()) {
            return;
        }

        Map<Character, Object> currentMap = map;
        for (int i = 0; i < word.length(); i++) {
            char c = Character.toLowerCase(word.charAt(i));
            Map<Character, Object> nextMap = (Map<Character, Object>) currentMap.get(c);

            if (nextMap == null) {
                nextMap = new ConcurrentHashMap<>();
                currentMap.put(c, nextMap);
            }

            currentMap = nextMap;

            if (i == word.length() - 1) {
                currentMap.put('$', Boolean.TRUE);
            }
        }
    }
}
