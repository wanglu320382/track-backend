/**
 * 问题溯源系统 - Web MVC 配置模块
 * <p>
 * 本文件配置 Spring MVC 的拦截器注册等，将认证拦截器应用到 /api 下的接口，并排除登录接口。
 * </p>
 */
package com.track.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类。
 * <p>
 * 实现 WebMvcConfigurer，注册认证拦截器：拦截 /api/**，放行 /api/auth/login。
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * 注册拦截器：对 /api 下所有请求进行认证校验，登录接口除外。
     *
     * @param registry 拦截器注册表，用于添加拦截器及路径规则
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }
}

