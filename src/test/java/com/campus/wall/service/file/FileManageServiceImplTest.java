package com.campus.wall.service.file;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.FileQueryDTO;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.enums.file.FileVisibility;
import com.campus.wall.service.file.FileAccessService;
import com.campus.wall.service.file.impl.FileManageServiceImpl;
import com.campus.wall.service.storage.StorageProvider;
import com.campus.wall.support.SaTokenTestContext;
import com.campus.wall.vo.file.FileCategoryVO;
import com.campus.wall.vo.file.FileVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileManageServiceImplTest {

    @Mock
    private FileRecordMapper fileRecordMapper;
    @Mock
    private FileAccessService fileAccessService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private FileManageServiceImpl fileManageService;

    private StpInterface previousStpInterface;

    @BeforeEach
    void setUpSaToken() {
        SaTokenTestContext.bind();
        StpUtil.login(1L);
        previousStpInterface = SaManager.getStpInterface();
        SaManager.setStpInterface(new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return List.of();
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                return List.of();
            }
        });
    }

    @AfterEach
    void tearDownSaToken() {
        StpUtil.logout();
        SaTokenTestContext.clear();
        SaManager.setStpInterface(previousStpInterface);
    }

    @Test
    void queryFiles_returnsUploaderAndUrl() {
        FileRecord record = record(10L, 2L, "a.png", "image/png", "PUBLIC");
        Page<FileRecord> page = new Page<>(1, 10);
        page.setRecords(List.of(record));
        page.setTotal(1);
        when(fileRecordMapper.selectPage(
            org.mockito.ArgumentMatchers.<Page<FileRecord>>any(),
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<FileRecord>>any()
        )).thenReturn(page);

        User user = new User();
        user.setId(2L);
        user.setNickname("Nick");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user));
        when(fileAccessService.buildAccessUrl(record)).thenReturn("/api/v1/files/preview/10");

        FileQueryDTO query = new FileQueryDTO();
        query.setPage(1);
        query.setSize(10);

        var result = fileManageService.queryFiles(query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        FileVO vo = result.getRecords().getFirst();
        assertThat(vo.getUploaderName()).isEqualTo("Nick");
        assertThat(vo.getUrl()).isEqualTo("/api/v1/files/preview/10");
    }

    @Test
    void listFileCategories_countsByMime() {
        FileRecord image = record(1L, 1L, "a.png", "image/png", "PUBLIC");
        FileRecord doc = record(2L, 1L, "a.pdf", "application/pdf", "PUBLIC");
        FileRecord zip = record(3L, 1L, "a.zip", "application/zip", "PUBLIC");
        when(fileRecordMapper.selectList(any())).thenReturn(List.of(image, doc, zip));

        List<FileCategoryVO> categories = fileManageService.listFileCategories();

        assertThat(categories).hasSize(7);
        assertThat(categories.getFirst().getKey()).isEqualTo("all");
        assertThat(categories.getFirst().getCount()).isEqualTo(3);
    }

    @Test
    void listGalleryCategories_unknownImageGroupedAsOther() {
        FileRecord webp = record(1L, 1L, "a.webp", "image/webp", "PUBLIC");
        FileRecord bmp = record(2L, 1L, "a.bmp", "image/bmp", "PUBLIC");
        when(fileRecordMapper.selectList(any())).thenReturn(List.of(webp, bmp));

        List<FileCategoryVO> categories = fileManageService.listGalleryCategories();

        assertThat(categories).hasSize(6);
        assertThat(categories.getFirst().getCount()).isEqualTo(2);
    }

    @Test
    void deleteFile_nullId_noop() {
        fileManageService.deleteFile(null);
        verify(fileRecordMapper, never()).selectById(any());
    }

    @Test
    void deleteFile_missingRecord_noop() {
        when(fileRecordMapper.selectById(10L)).thenReturn(null);
        fileManageService.deleteFile(10L);
        verify(fileRecordMapper, never()).deleteById(any());
    }

    @Test
    void deleteFile_success_deletesStorageThenDb() throws Exception {
        FileRecord record = record(10L, 2L, "a.png", "image/png", "PUBLIC");
        record.setPath("uploads/a.png");
        record.setStorageProvider("LOCAL");
        when(fileRecordMapper.selectById(10L)).thenReturn(record);
        StorageProvider provider = org.mockito.Mockito.mock(StorageProvider.class);
        when(fileAccessService.getProvider("LOCAL")).thenReturn(provider);

        fileManageService.deleteFile(10L);

        verify(provider).delete("uploads/a.png");
        verify(fileRecordMapper).deleteById(10L);
    }

    @Test
    void deleteFile_storageFailure_throwsInternalError() throws Exception {
        FileRecord record = record(10L, 2L, "a.png", "image/png", "PUBLIC");
        record.setPath("uploads/a.png");
        record.setStorageProvider("LOCAL");
        when(fileRecordMapper.selectById(10L)).thenReturn(record);
        StorageProvider provider = org.mockito.Mockito.mock(StorageProvider.class);
        when(fileAccessService.getProvider("LOCAL")).thenReturn(provider);
        doThrow(new RuntimeException("boom")).when(provider).delete("uploads/a.png");

        assertThatThrownBy(() -> fileManageService.deleteFile(10L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.INTERNAL_ERROR.getCode());
    }

    @Test
    void updateVisibility_missingRecord_notFound() {
        when(fileRecordMapper.selectById(20L)).thenReturn(null);
        assertThatThrownBy(() -> fileManageService.updateVisibility(20L, "PUBLIC"))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void updateVisibility_sameValue_noop() {
        FileRecord record = record(20L, 2L, "a.png", "image/png", "PUBLIC");
        when(fileRecordMapper.selectById(20L)).thenReturn(record);

        fileManageService.updateVisibility(20L, "public");

        verify(fileRecordMapper, never()).updateById(any());
    }

    @Test
    void updateVisibility_invalidValue_throwsBadRequest() {
        assertThatThrownBy(() -> fileManageService.updateVisibility(20L, "INVALID"))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
        verify(fileRecordMapper, never()).selectById(any());
    }

    @Test
    void updateVisibility_success_updatesRecord() {
        FileRecord record = record(20L, 2L, "a.png", "image/png", "PRIVATE");
        when(fileRecordMapper.selectById(20L)).thenReturn(record);

        fileManageService.updateVisibility(20L, "PUBLIC");

        assertThat(record.getVisibility()).isEqualTo(FileVisibility.PUBLIC.getCode());
        verify(fileRecordMapper).updateById(record);
    }

    private FileRecord record(Long id, Long userId, String filename, String mimeType, String visibility) {
        FileRecord record = new FileRecord();
        record.setId(id);
        record.setUserId(userId);
        record.setFilename(filename);
        record.setMimeType(mimeType);
        record.setVisibility(visibility);
        return record;
    }
}
