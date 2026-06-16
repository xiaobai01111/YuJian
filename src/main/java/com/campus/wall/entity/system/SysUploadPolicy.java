package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_upload_policies")
public class SysUploadPolicy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sceneCode;
    private String sceneName;
    private String assetType;
    private String visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
