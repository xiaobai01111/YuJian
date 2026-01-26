package com.campus.wall.config;

import com.campus.wall.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 注意：CORS 配置已移至 SaTokenConfig，避免重复配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        boolean usesLocal = storageProperties.getPrimaryProvider() == com.campus.wall.enums.file.StorageProviderType.LOCAL
                || storageProperties.getFallbackProvider() == com.campus.wall.enums.file.StorageProviderType.LOCAL;
        if (!usesLocal || !storageProperties.isLocalPublicEnabled()) {
            return;
        }
        String path = storageProperties.getLocalPath();
        java.nio.file.Path basePath = java.nio.file.Paths.get(path);
        if (!basePath.isAbsolute()) {
            basePath = java.nio.file.Paths.get(System.getProperty("user.dir")).resolve(basePath);
        }
        String location = "file:" + basePath.normalize().toString() + "/";
        String urlPrefix = storageProperties.getLocalUrlPrefix();
        String pattern = urlPrefix.endsWith("/") ? urlPrefix + "**" : urlPrefix + "/**";
        registry.addResourceHandler(pattern).addResourceLocations(location);
    }
}
