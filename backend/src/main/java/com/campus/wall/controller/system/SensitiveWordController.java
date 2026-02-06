package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.R;
import com.campus.wall.common.PageResult;
import com.campus.wall.entity.system.SensitiveWord;
import com.campus.wall.mapper.system.SensitiveWordMapper;
import com.campus.wall.service.content.SensitiveWordService;
import com.campus.wall.service.system.OperLogService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 敏感词管理控制器
 */
@RestController
@RequiredArgsConstructor
public class SensitiveWordController {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWordService sensitiveWordService;
    private final OperLogService operLogService;

    private static final int MIN_WORD_LENGTH = 2;
    private static final int BATCH_CHUNK_SIZE = 500;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int LEVEL_BLOCK = 2; // 只使用拦截级别

    /**
     * 查询敏感词列表
     */
    @SaCheckLogin
    @GetMapping("/api/v1/system/sensitive-words")
    public R<PageResult<SensitiveWordVO>> query(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String keyword) {
        
        // 限制分页大小
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 1);
        
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (level != null) {
            wrapper.eq(SensitiveWord::getLevel, level);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(SensitiveWord::getWord, keyword.trim());
        }
        wrapper.orderByDesc(SensitiveWord::getCreatedAt);
        
        Page<SensitiveWord> result = sensitiveWordMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<SensitiveWordVO> voList = result.getRecords().stream().map(this::toVO).toList();
        
        return R.ok(PageResult.of(voList, result.getTotal(), safeSize, safePage));
    }

    /**
     * 新增敏感词
     */
    @SaCheckLogin
    @PostMapping("/api/v1/system/sensitive-words")
    public R<SensitiveWordVO> create(@RequestBody SensitiveWordDTO dto) {
        String word = normalizeWord(dto.getWord());
        if (word == null || word.length() < MIN_WORD_LENGTH) {
            return R.fail("敏感词长度不能少于" + MIN_WORD_LENGTH + "个字符");
        }
        
        // 检查重复
        if (existsWord(word)) {
            return R.fail("敏感词已存在");
        }
        
        int level = normalizeLevel(dto.getLevel());
        
        SensitiveWord entity = new SensitiveWord();
        entity.setWord(word);
        entity.setLevel(level);
        entity.setCreatedAt(LocalDateTime.now());
        sensitiveWordMapper.insert(entity);
        
        // 记录操作日志
        operLogService.log("敏感词", entity.getId(), "新增", "新增敏感词: " + word);
        
        // 刷新内存词库
        sensitiveWordService.reloadSensitiveWords();
        
        return R.ok(toVO(entity));
    }

    /**
     * 批量导入敏感词
     */
    @SaCheckLogin
    @PostMapping("/api/v1/system/sensitive-words/batch")
    public R<BatchImportResult> createBatch(@RequestBody BatchImportDTO dto) {
        if (dto.getWords() == null || dto.getWords().isEmpty()) {
            return R.fail("词列表不能为空");
        }
        
        int level = normalizeLevel(dto.getLevel());
        List<String> added = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> invalid = new ArrayList<>();
        
        Set<String> normalizedWords = new HashSet<>();
        List<String> pending = new ArrayList<>();
        for (String rawWord : dto.getWords()) {
            String word = normalizeWord(rawWord);
            if (word == null || word.length() < MIN_WORD_LENGTH) {
                if (rawWord != null && !rawWord.trim().isEmpty()) {
                    invalid.add(rawWord.trim());
                }
                continue;
            }
            if (!normalizedWords.add(word)) {
                skipped.add(word);
                continue;
            }
            pending.add(word);
        }

        for (int i = 0; i < pending.size(); i += BATCH_CHUNK_SIZE) {
            List<String> chunk = pending.subList(i, Math.min(i + BATCH_CHUNK_SIZE, pending.size()));
            Set<String> existingWords = new HashSet<>();
            if (!chunk.isEmpty()) {
                List<SensitiveWord> existing = sensitiveWordMapper.selectList(
                        new LambdaQueryWrapper<SensitiveWord>()
                                .in(SensitiveWord::getWord, chunk)
                );
                for (SensitiveWord w : existing) {
                    existingWords.add(w.getWord().toLowerCase());
                }
            }
            LocalDateTime now = LocalDateTime.now();
            for (String word : chunk) {
                if (existingWords.contains(word.toLowerCase())) {
                    skipped.add(word);
                    continue;
                }
                SensitiveWord entity = new SensitiveWord();
                entity.setWord(word);
                entity.setLevel(level);
                entity.setCreatedAt(now);
                sensitiveWordMapper.insert(entity);
                added.add(word);
            }
        }
        
        // 记录操作日志
        if (!added.isEmpty()) {
            operLogService.log("敏感词", null, "批量导入", 
                "新增" + added.size() + "条，跳过" + skipped.size() + "条，无效" + invalid.size() + "条");
            sensitiveWordService.reloadSensitiveWords();
        }
        
        BatchImportResult result = new BatchImportResult();
        result.setAddedCount(added.size());
        result.setSkippedCount(skipped.size());
        result.setInvalidCount(invalid.size());
        result.setAdded(added);
        result.setSkipped(skipped);
        result.setInvalid(invalid);
        
        return R.ok(result);
    }

    /**
     * 删除敏感词
     */
    @SaCheckLogin
    @DeleteMapping("/api/v1/system/sensitive-words/{id}")
    public R<Void> delete(@PathVariable Long id) {
        SensitiveWord word = sensitiveWordMapper.selectById(id);
        if (word == null) {
            return R.fail("敏感词不存在");
        }
        sensitiveWordMapper.deleteById(id);
        
        // 记录操作日志
        operLogService.log("敏感词", id, "删除", "删除敏感词: " + word.getWord());
        
        sensitiveWordService.reloadSensitiveWords();
        return R.ok();
    }

    /**
     * 批量删除敏感词
     */
    @SaCheckLogin
    @DeleteMapping("/api/v1/system/sensitive-words")
    public R<Integer> deleteBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return R.fail("ID列表不能为空");
        }
        int deleted = sensitiveWordMapper.deleteBatchIds(ids);
        if (deleted > 0) {
            // 记录操作日志
            operLogService.log("敏感词", null, "批量删除", "删除" + deleted + "条敏感词");
            sensitiveWordService.reloadSensitiveWords();
        }
        return R.ok(deleted);
    }

    /**
     * 更新敏感词级别
     */
    @SaCheckLogin
    @PutMapping("/api/v1/system/sensitive-words/{id}")
    public R<SensitiveWordVO> update(@PathVariable Long id, @RequestBody SensitiveWordDTO dto) {
        SensitiveWord word = sensitiveWordMapper.selectById(id);
        if (word == null) {
            return R.fail("敏感词不存在");
        }
        if (dto.getLevel() != null) {
            int oldLevel = word.getLevel();
            int level = normalizeLevel(dto.getLevel());
            word.setLevel(level);
            sensitiveWordMapper.updateById(word);
            
            // 记录操作日志
            operLogService.log("敏感词", id, "修改级别", 
                "敏感词[" + word.getWord() + "]级别从" + oldLevel + "改为" + level);
            sensitiveWordService.reloadSensitiveWords();
        }
        return R.ok(toVO(word));
    }

    private String normalizeWord(String word) {
        if (word == null) return null;
        return word.trim().toLowerCase();
    }

    private int normalizeLevel(Integer level) {
        // 已移除警告级别，统一使用拦截
        return LEVEL_BLOCK;
    }

    private boolean existsWord(String word) {
        return sensitiveWordMapper.selectCount(
                new LambdaQueryWrapper<SensitiveWord>()
                        .eq(SensitiveWord::getWord, word)
        ) > 0;
    }

    private SensitiveWordVO toVO(SensitiveWord entity) {
        SensitiveWordVO vo = new SensitiveWordVO();
        vo.setId(entity.getId());
        vo.setWord(entity.getWord());
        vo.setLevel(entity.getLevel());
        vo.setLevelText(getLevelText(entity.getLevel()));
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private String getLevelText(Integer level) {
        if (level == null) return "未知";
        return switch (level) {
            case 1 -> "警告";
            case 2 -> "拦截";
            default -> "未知";
        };
    }

    @Data
    public static class SensitiveWordVO {
        private Long id;
        private String word;
        private Integer level;
        private String levelText;
        private LocalDateTime createdAt;
    }

    @Data
    public static class SensitiveWordDTO {
        private String word;
        private Integer level;
    }

    @Data
    public static class BatchImportDTO {
        private List<String> words;
        private Integer level;
    }

    @Data
    public static class BatchImportResult {
        private int addedCount;
        private int skippedCount;
        private int invalidCount;
        private List<String> added;
        private List<String> skipped;
        private List<String> invalid;
    }
}
