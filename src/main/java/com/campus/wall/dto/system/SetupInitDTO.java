package com.campus.wall.dto.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetupInitDTO {

    @NotBlank
    private String adminUsername;

    @NotBlank
    @Size(min = 8, max = 32, message = "管理员密码长度为8-32个字符")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,32}$",
            message = "管理员密码需包含大小写字母、数字和特殊字符")
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
