package com.campus.wall.property;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 敏感词过滤属性测试
 * Property 16: 敏感词拦截
 */
class SensitiveWordPropertyTest {

    private static final Character END_FLAG = '$';

    /**
     * Property 16: 包含敏感词的文本应被检测到
     */
    @Property(tries = 100)
    void textContainingSensitiveWordIsDetected(
            @ForAll("sensitiveWord") String sensitiveWord,
            @ForAll("normalText") String prefix,
            @ForAll("normalText") String suffix) {
        Map<Character, Object> wordMap = buildWordMap(List.of(sensitiveWord));
        String text = prefix + sensitiveWord + suffix;

        boolean detected = containsSensitiveWord(text, wordMap);

        assertThat(detected).isTrue();
    }

    /**
     * Property 16: 不包含敏感词的文本应通过
     */
    @Property(tries = 100)
    void textWithoutSensitiveWordPasses(
            @ForAll("normalText") String text) {
        List<String> sensitiveWords = List.of("敏感词", "违规", "禁止");
        Map<Character, Object> wordMap = buildWordMap(sensitiveWords);

        // 确保测试文本不包含敏感词
        Assume.that(!text.contains("敏感词") && !text.contains("违规") && !text.contains("禁止"));

        boolean detected = containsSensitiveWord(text, wordMap);

        assertThat(detected).isFalse();
    }

    /**
     * Property 16: 敏感词替换后应不再包含原词
     */
    @Property(tries = 100)
    void replacedTextDoesNotContainSensitiveWord(
            @ForAll("sensitiveWord") String sensitiveWord,
            @ForAll("normalText") String prefix,
            @ForAll("normalText") String suffix) {
        String text = prefix + sensitiveWord + suffix;
        Map<Character, Object> wordMap = buildWordMap(List.of(sensitiveWord));

        String replaced = replaceSensitiveWords(text, '*', wordMap);

        assertThat(replaced).doesNotContain(sensitiveWord);
    }

    /**
     * Property 16: 替换字符数量应等于敏感词长度
     */
    @Property(tries = 100)
    void replacementLengthMatchesSensitiveWordLength(
            @ForAll("sensitiveWord") String sensitiveWord) {
        String text = "前缀" + sensitiveWord + "后缀";
        Map<Character, Object> wordMap = buildWordMap(List.of(sensitiveWord));

        String replaced = replaceSensitiveWords(text, '*', wordMap);
        
        int starCount = (int) replaced.chars().filter(c -> c == '*').count();
        assertThat(starCount).isEqualTo(sensitiveWord.length());
    }

    /**
     * Property 16: 空文本应通过检测
     */
    @Example
    void emptyTextPasses() {
        Map<Character, Object> wordMap = buildWordMap(List.of("敏感词"));
        
        assertThat(containsSensitiveWord("", wordMap)).isFalse();
        assertThat(containsSensitiveWord(null, wordMap)).isFalse();
    }

    /**
     * Property 16: 大小写不敏感匹配
     */
    @Property(tries = 50)
    void caseInsensitiveMatching(
            @ForAll("englishSensitiveWord") String word) {
        Map<Character, Object> wordMap = buildWordMap(List.of(word.toLowerCase()));

        boolean lowerDetected = containsSensitiveWord(word.toLowerCase(), wordMap);
        boolean upperDetected = containsSensitiveWord(word.toUpperCase(), wordMap);
        boolean mixedDetected = containsSensitiveWord(mixCase(word), wordMap);

        assertThat(lowerDetected).isTrue();
        assertThat(upperDetected).isTrue();
        assertThat(mixedDetected).isTrue();
    }

    // DFA 算法实现
    private Map<Character, Object> buildWordMap(List<String> words) {
        Map<Character, Object> map = new ConcurrentHashMap<>();
        for (String word : words) {
            addWordToMap(word, map);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private void addWordToMap(String word, Map<Character, Object> map) {
        if (word == null || word.isEmpty()) return;
        
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

    private boolean containsSensitiveWord(String text, Map<Character, Object> wordMap) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            if (checkSensitiveWord(text, i, wordMap) > 0) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private int checkSensitiveWord(String text, int beginIndex, Map<Character, Object> wordMap) {
        Map<Character, Object> currentMap = wordMap;
        int matchLength = 0;
        int tempLength = 0;

        for (int i = beginIndex; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            Map<Character, Object> nextMap = (Map<Character, Object>) currentMap.get(c);
            if (nextMap == null) break;
            tempLength++;
            if (Boolean.TRUE.equals(nextMap.get('$'))) {
                matchLength = tempLength;
            }
            currentMap = nextMap;
        }
        return matchLength;
    }

    private String replaceSensitiveWords(String text, char replacement, Map<Character, Object> wordMap) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder result = new StringBuilder(text);
        for (int i = 0; i < result.length(); i++) {
            int length = checkSensitiveWord(result.toString(), i, wordMap);
            if (length > 0) {
                for (int j = i; j < i + length; j++) {
                    result.setCharAt(j, replacement);
                }
                i = i + length - 1;
            }
        }
        return result.toString();
    }

    private String mixCase(String word) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            sb.append(i % 2 == 0 ? Character.toLowerCase(c) : Character.toUpperCase(c));
        }
        return sb.toString();
    }

    // 数据提供者
    @Provide
    Arbitrary<String> sensitiveWord() {
        return Arbitraries.of("敏感词", "违规内容", "禁止发布", "不良信息");
    }

    @Provide
    Arbitrary<String> normalText() {
        return Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(20)
                .alpha()
                .numeric();
    }

    @Provide
    Arbitrary<String> englishSensitiveWord() {
        return Arbitraries.of("badword", "spam", "abuse", "blocked");
    }
}
