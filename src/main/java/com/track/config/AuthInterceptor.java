/**
 * 问题溯源系统 - 认证拦截器模块
 * <p>
 * 本文件实现基于令牌的登录校验拦截器，对 /api 下非登录接口校验 X-Auth-Token，未通过时返回 401 JSON。
 * </p>
 */
package com.track.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.track.common.AuthTokenService;
import com.track.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证拦截器：校验请求头中的登录令牌，放行登录接口，未登录时返回 401。
 * <p>
 * 通过 X-Auth-Token 从 AuthTokenService 解析用户，成功则将当前用户放入 request 属性 currentUser。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthTokenService authTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 在请求进入控制器前执行：校验令牌，放行登录接口，未登录则写 401 并中断。
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler  目标处理器
     * @return true 放行，false 已写 401 并中断
     * @throws Exception 写响应时的 IO 等异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        // 放行登录接口，避免拦截登录请求
        if (path.startsWith("/api/auth/login")) {
            return true;
        }
        String token = request.getHeader("X-Auth-Token");
        SysUser user = authTokenService.getUserByToken(token);
        if (user == null) {
            writeUnauthorized(response);
            return false;
        }
        // 将当前用户放入 request，供 Controller 使用
        request.setAttribute("currentUser", user);
        return true;
    }

    /**
     * 向响应中写入 401 未授权 JSON 体（code=401, message=未登录或登录已过期）。
     *
     * @param response HTTP 响应对象
     * @throws IOException 写输出流时的异常
     */
    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new HashMap<>();
        body.put("code", 401);
        body.put("message", "未登录或登录已过期");
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

