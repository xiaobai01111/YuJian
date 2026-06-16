package com.campus.wall.service.system;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.UploadPolicyUpdateDTO;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.entity.system.SysUploadPolicy;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.mapper.system.SysUploadPolicyMapper;
import com.campus.wall.service.system.impl.UploadPolicyServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadPolicyServiceImplTest {

    @Mock
    private SysUploadPolicyMapper uploadPolicyMapper;
    @Mock
    private FileRecordMapper fileRecordMapper;
    @Mock
    private OperLogService operLogService;

    @InjectMocks
    private UploadPolicyServiceImpl uploadPolicyService;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysUploadPolicy.class);
        TableInfoHelper.initTableInfo(assistant, FileRecord.class);
    }

    @Test
    void updatePolicy_idCardScene_forbidden() {
        SysUploadPolicy idCardPolicy = new SysUploadPolicy();
        idCardPolicy.setId(1L);
        idCardPolicy.setSceneCode("id_card");
        idCardPolicy.setAssetType("resource");
        idCardPolicy.setVisibility("PRIVATE");
        when(uploadPolicyMapper.selectOne(any())).thenReturn(idCardPolicy);

        UploadPolicyUpdateDTO dto = new UploadPolicyUpdateDTO();
        dto.setAssetType("gallery");
        dto.setVisibility("PUBLIC");

        assertThatThrownBy(() -> uploadPolicyService.updatePolicy("id_card", dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode())
            .hasMessage("身份材料策略不允许修改");

        verify(uploadPolicyMapper, never()).updateById(any());
    }

    @Test
    void findByScene_idCardScene_autoFixPolicy() {
        SysUploadPolicy idCardPolicy = new SysUploadPolicy();
        idCardPolicy.setId(2L);
        idCardPolicy.setSceneCode("id_card");
        idCardPolicy.setAssetType("gallery");
        idCardPolicy.setVisibility("PUBLIC");
        when(uploadPolicyMapper.selectOne(any())).thenReturn(idCardPolicy);

        SysUploadPolicy result = uploadPolicyService.findByScene("id_card");

        assertThat(result).isNotNull();
        assertThat(result.getAssetType()).isEqualTo("resource");
        assertThat(result.getVisibility()).isEqualTo("PRIVATE");
        verify(uploadPolicyMapper).updateById(idCardPolicy);
    }

    @Test
    void updatePolicy_resourceScene_updatesResourceAndReEnforcesIdCardPrivate() {
        SysUploadPolicy resourcePolicy = new SysUploadPolicy();
        resourcePolicy.setId(3L);
        resourcePolicy.setSceneCode("resource");
        resourcePolicy.setAssetType("resource");
        resourcePolicy.setVisibility("PRIVATE");
        when(uploadPolicyMapper.selectOne(any())).thenReturn(resourcePolicy);

        UploadPolicyUpdateDTO dto = new UploadPolicyUpdateDTO();
        dto.setAssetType("resource");
        dto.setVisibility("PUBLIC");

        uploadPolicyService.updatePolicy("resource", dto);

        verify(fileRecordMapper, times(2)).update(org.mockito.Mockito.isNull(), any());
    }
}
