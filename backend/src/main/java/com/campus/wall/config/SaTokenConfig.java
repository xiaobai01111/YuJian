package com.campus.wall.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${cors.allowed-origin-patterns:}")
    private String allowedOriginPatterns;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 跳过 OPTIONS 预检请求
            String method = SaHolder.getRequest().getMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                return;
            }
            // 公开接口白名单
            SaRouter.match("/**")
                    .notMatch(
                            // 静态资源
                            "/favicon.ico",
                            "/error",
                            // API 文档
                            "/doc.html",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/swagger-resources/**",
                            "/v3/api-docs/**",
                            "/webjars/**",
                            // 认证接口
                            "/api/v1/auth/register",
                            "/api/v1/auth/login",
                            "/api/v1/auth/logout",
                            // 公告公开接口
                            "/api/v1/notices/public",
                            "/api/v1/notices/public/**",
                            // 公开查询接口（仅健康检查，其他需登录）
                            "/api/health",
                            "/api/v1/posts"
                    )
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        var registration = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "X-Trace-Id")
                .allowCredentials(true)
                .maxAge(3600);

        String[] origins = splitAndTrim(allowedOrigins);
        if (origins.length > 0) {
            registration.allowedOrigins(origins);
        }
        String[] patterns = splitAndTrim(allowedOriginPatterns);
        if (patterns.length > 0) {
            registration.allowedOriginPatterns(patterns);
        }
    }

    private String[] splitAndTrim(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new String[0];
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toArray(String[]::new);
    }
}
