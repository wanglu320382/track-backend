/**
 * 问题溯源系统 - 跨域配置模块
 * <p>
 * 本文件提供 Spring Web 的 CORS（跨域资源共享）配置，允许前端应用跨域访问后端 API。
 * </p>
 */
package com.track.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置类。
 * <p>
 * 注册 CORS 过滤器，对所有路径（/**）启用跨域支持，便于前后端分离部署时前端调用后端接口。
 * </p>
 */
@Configuration
public class CorsConfig {

    /**
     * 创建并注册 CORS 过滤器 Bean。
     * <p>
     * 允许任意来源、任意请求头、任意 HTTP 方法，并允许携带凭证（如 Cookie）。
     * </p>
     *
     * @return 应用于全局的 CorsFilter 实例
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许任意来源（生产环境建议按需限制）
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
