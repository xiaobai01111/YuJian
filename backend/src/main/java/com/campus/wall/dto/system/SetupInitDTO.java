package com.campus.wall.dto.system;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SetupInitDTO {

    @NotBlank
    private String adminUsername;

    @NotBlank
    private String adminPassword;

    @NotBlank
    private String adminConfirmPassword;

    private String adminNickname;

    private String adminEmail;

    @NotBlank
    private String siteName;

    private String logoUrl;

    private String faviconUrl;

    private String theme;

    private String storageProvider;

    private String localPath;

    private Boolean localPublicEnabled;
}
