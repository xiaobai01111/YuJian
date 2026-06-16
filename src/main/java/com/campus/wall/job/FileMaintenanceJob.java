package com.campus.wall.job;

import com.campus.wall.service.file.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileMaintenanceJob {

    private final FileService fileService;

    @Scheduled(cron = "${app.file.orphan-clean-cron:0 30 3 * * ?}")
    public void cleanOrphanFiles() {
        try {
            fileService.cleanOrphanFiles();
        } catch (Exception e) {
            log.error("定时清理孤儿文件失败", e);
        }
    }
}
