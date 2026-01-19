package com.campus.wall.service.file;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.FileQueryDTO;
import com.campus.wall.vo.file.FileCategoryVO;
import com.campus.wall.vo.file.FileVO;

import java.util.List;

/**
 * 文件管理服务
 */
public interface FileManageService {

    PageResult<FileVO> queryFiles(FileQueryDTO query);

    List<FileCategoryVO> listFileCategories();

    PageResult<FileVO> queryGallery(FileQueryDTO query);

    List<FileCategoryVO> listGalleryCategories();

    void deleteFile(Long fileId);

    void deleteFiles(List<Long> fileIds);

    void updateVisibility(Long fileId, String visibility);
}
