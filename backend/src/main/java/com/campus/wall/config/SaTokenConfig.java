package com.campus.wall.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
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
                            // 公开查询接口
                            "/api/health",
                            "/api/v1/posts",
                            "/api/v1/posts/{id}",
                            "/api/v1/comments/post/**",
                            "/api/v1/search",
                            "/api/v1/announcements"
                    )
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
