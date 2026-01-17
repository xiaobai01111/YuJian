package com.campus.wall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 注意：CORS 配置已移至 SaTokenConfig，避免重复配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // CORS 配置已在 SaTokenConfig 中统一处理
}
