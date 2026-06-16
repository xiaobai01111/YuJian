package com.campus.wall.service.file;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.constant.RateLimitConstants;
import com.campus.wall.config.FileCleanupProperties;
import com.campus.wall.config.StorageProperties;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.entity.system.SysUploadPolicy;
import com.campus.wall.enums.file.StorageProviderType;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.service.content.ContentModerationService;
import com.campus.wall.service.security.RateLimitService;
import com.campus.wall.service.system.UploadPolicyService;
import com.campus.wall.service.file.impl.FileServiceImpl;
import com.campus.wall.service.storage.StorageProvider;
import com.campus.wall.service.storage.StorageProviderRegistry;
import com.campus.wall.support.SaTokenTestContext;
import jakarta.servlet.http.HttpServletRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    private UploadPolicyService uploadPolicyService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private StorageProvider storageProvider;

    @InjectMocks
    private FileServiceImpl fileService;

    private StpInterface previousStpInterface;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        StpUtil.login(1L);
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
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
        when(contentModerationService.scanFile(any())).thenReturn(true);
        when(fileRecordMapper.insert(any())).thenThrow(new RuntimeException("db fail"));

        assertThatThrownBy(() -> fileService.uploadFile(file, "post", "PUBLIC", "post"))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FILE_UPLOAD_FAILED.getCode());

        verify(rateLimitService).checkRateLimit(
            "rate:file-upload:user:1",
            RateLimitConstants.FILE_UPLOAD_LIMIT_PER_MINUTE,
            RateLimitConstants.WINDOW_SECONDS,
            ResultCode.TOO_MANY_REQUESTS,
            "上传过于频繁，请稍后再试"
        );
        verify(rateLimitService).checkRateLimit(
            "rate:file-upload:ip:127.0.0.1",
            RateLimitConstants.FILE_UPLOAD_LIMIT_PER_MINUTE_PER_IP,
            RateLimitConstants.WINDOW_SECONDS,
            ResultCode.TOO_MANY_REQUESTS,
            "上传过于频繁，请稍后再试"
        );
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

    @Test
    void uploadFile_idCardScene_forcesPrivateAndResource() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "id-card.png", "image/png", buildPngBytes()
        );
        SysUploadPolicy policy = new SysUploadPolicy();
        policy.setSceneCode("id_card");
        policy.setAssetType("gallery");
        policy.setVisibility("PUBLIC");

        when(uploadPolicyService.findByScene("id_card")).thenReturn(policy);
        when(storageProperties.getPrimaryProvider()).thenReturn(StorageProviderType.LOCAL);
        when(storageProviderRegistry.getProvider(StorageProviderType.LOCAL)).thenReturn(storageProvider);
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(storageProvider.store(any(), any())).thenReturn("test/id-card.png");
        when(contentModerationService.scanFile(any())).thenReturn(true);
        doAnswer(invocation -> {
            FileRecord record = invocation.getArgument(0);
            record.setId(88L);
            return 1;
        }).when(fileRecordMapper).insert(any(FileRecord.class));

        fileService.uploadFile(file, "id_card", "PUBLIC", "id_card");

        var recordCaptor = org.mockito.ArgumentCaptor.forClass(FileRecord.class);
        verify(fileRecordMapper).insert(recordCaptor.capture());
        FileRecord saved = recordCaptor.getValue();
        assertThat(saved.getVisibility()).isEqualTo("PRIVATE");
        assertThat(saved.getAssetType()).isEqualTo("resource");
        verify(contentModerationService, never()).moderateImage(any());
    }

    private byte[] buildPngBytes() {
        return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
    }
}
