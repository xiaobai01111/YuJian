package com.campus.wall.vo.system;

import lombok.Data;

@Data
public class SetupStatusVO {
    private boolean setupCompleted;
    private String siteName;
    private String storageProvider;
    private String localPath;
    private Boolean localPublicEnabled;
}
