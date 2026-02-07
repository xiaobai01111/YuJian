package com.campus.wall.service.file;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.config.FileCleanupProperties;
import com.campus.wall.config.StorageProperties;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.enums.file.StorageProviderType;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.service.content.ContentModerationService;
import com.campus.wall.service.file.impl.FileServiceImpl;
import com.campus.wall.service.storage.StorageProvider;
import com.campus.wall.service.storage.StorageProviderRegistry;
import com.campus.wall.support.SaTokenTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRecordMapper fileRecordMapper;
    @Mock
    private ContentModerationService contentModerationService;
    @Mock
    private StorageProviderRegistry storageProviderRegistry;
    @Mock
    private StorageProperties storageProperties;
    @Mock
    private FileAccessService fileAccessService;
    @Mock
    private FileCleanupProperties cleanupProperties;
    @Mock
    private StorageProvider storageProvider;

    @InjectMocks
    private FileServiceImpl fileService;

    private StpInterface previousStpInterface;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        StpUtil.login(1L);
        previousStpInterface = SaManager.getStpInterface();
        SaManager.setStpInterface(new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return Collections.emptyList();
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                return Collections.emptyList();
            }
        });
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
        SaManager.setStpInterface(previousStpInterface);
    }

    @Test
    void uploadFile_insertFails_cleansStoredObject() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.png", "image/png", buildPngBytes()
        );
        when(storageProperties.getPrimaryProvider()).thenReturn(StorageProviderType.LOCAL);
        when(storageProviderRegistry.getProvider(StorageProviderType.LOCAL)).thenReturn(storageProvider);
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(storageProvider.store(any(), any())).thenReturn("test/path.png");
        when(fileRecordMapper.insert(any())).thenThrow(new RuntimeException("db fail"));

        assertThatThrownBy(() -> fileService.uploadFile(file, "post", "PUBLIC"))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FILE_UPLOAD_FAILED.getCode());

        verify(storageProvider).delete("test/path.png");
    }

    @Test
    void deleteFile_notOwner_throwsForbidden() {
        FileRecord record = new FileRecord();
        record.setId(10L);
        record.setUserId(2L);
        record.setStorageProvider(StorageProviderType.LOCAL.getCode());
        record.setPath("test/path.png");

        when(fileRecordMapper.selectById(10L)).thenReturn(record);

        assertThatThrownBy(() -> fileService.deleteFile(10L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void deleteFile_missingRecord_noop() {
        when(fileRecordMapper.selectById(99L)).thenReturn(null);

        fileService.deleteFile(99L);

        verify(fileRecordMapper, never()).deleteById(any());
    }

    @Test
    void deleteFile_ownerDeletesStorageAndRecord() throws Exception {
        FileRecord record = new FileRecord();
        record.setId(11L);
        record.setUserId(1L);
        record.setStorageProvider(StorageProviderType.LOCAL.getCode());
        record.setPath("test/path.png");

        when(fileRecordMapper.selectById(11L)).thenReturn(record);
        when(fileAccessService.getProvider(StorageProviderType.LOCAL.getCode())).thenReturn(storageProvider);

        fileService.deleteFile(11L);

        verify(storageProvider).delete("test/path.png");
        verify(fileRecordMapper).deleteById(11L);
    }

    private byte[] buildPngBytes() {
        return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
    }
}
