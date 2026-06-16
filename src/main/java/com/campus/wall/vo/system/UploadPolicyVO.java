package com.campus.wall.vo.system;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UploadPolicyVO {
    private String sceneCode;
    private String sceneName;
    private String assetType;
    private String visibility;
    private LocalDateTime updatedAt;
}
