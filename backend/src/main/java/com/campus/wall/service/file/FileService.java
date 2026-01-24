package com.campus.wall.service.file;

import com.campus.wall.vo.file.FileVO;
import com.campus.wall.dto.system.FileCleanupRequestDTO;
import com.campus.wall.vo.system.FileCleanupConfigVO;
import com.campus.wall.vo.system.FileCleanupResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 上传文件
     */
    FileVO uploadFile(MultipartFile file, String targetType, String visibility);

    /**
     * 删除文件
     */
    void deleteFile(Long fileId);

    /**
     * 绑定文件到目标
     */
    void bindFiles(List<Long> fileIds, Long targetId, String targetType);

    /**
     * 获取目标关联的文件
     */
    List<FileVO> getFilesByTarget(Long targetId, String targetType);

    /**
     * 清理孤儿文件
     */
    void cleanOrphanFiles();

    /**
     * 获取清理策略配置
     */
    FileCleanupConfigVO getCleanupConfig();

    /**
     * 更新清理策略配置
     */
    FileCleanupConfigVO updateCleanupConfig(FileCleanupRequestDTO dto);

    /**
     * 手动清理孤儿文件
     */
    FileCleanupResultVO cleanOrphanFiles(FileCleanupRequestDTO dto);
}
