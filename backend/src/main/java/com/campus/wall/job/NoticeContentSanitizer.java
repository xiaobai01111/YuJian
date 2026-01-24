package com.campus.wall.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.entity.system.SysNotice;
import com.campus.wall.mapper.system.SysNoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notice", name = "sanitize-on-startup", havingValue = "true")
public class NoticeContentSanitizer implements ApplicationRunner {

    private final SysNoticeMapper noticeMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SysNotice> notices = noticeMapper.selectList(
                new LambdaQueryWrapper<SysNotice>()
                        .select(SysNotice::getId, SysNotice::getContent)
        );
        int updated = 0;
        for (SysNotice notice : notices) {
            String content = notice.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            String cleaned = Jsoup.clean(content, Safelist.basic());
            if (!cleaned.equals(content)) {
                SysNotice update = new SysNotice();
                update.setId(notice.getId());
                update.setContent(cleaned);
                update.setUpdatedAt(LocalDateTime.now());
                noticeMapper.updateById(update);
                updated++;
            }
        }
        log.info("公告内容清洗完成，更新 {} 条记录", updated);
    }
}
