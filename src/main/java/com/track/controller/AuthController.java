package com.track.controller;

import com.track.common.AuthTokenService;
import com.track.common.Result;
import com.track.entity.SysUser;
import com.track.service.SysUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（REST API 入口）。
 * <p>
 * 提供登录、退出等认证相关接口。
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final AuthTokenService authTokenService;

    /**
     * 用户登录，校验用户名密码后返回令牌及用户信息。
     *
     * @param request 用户名、密码
     * @return 令牌、用户名、昵称、角色
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        SysUser user = sysUserService.login(request.getUsername(), request.getPassword());
        String token = authTokenService.generateToken(user);
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setRole(user.getRole());
        return Result.success(resp);
    }

    /**
     * 用户退出，使当前令牌失效。
     *
     * @param token 请求头 X-Auth-Token 中的令牌（可选）
     * @return 无内容
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        authTokenService.removeToken(token);
        return Result.success(null);
    }

    /** 登录请求体 */
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    /** 登录成功响应（含令牌与用户信息） */
    @Data
    public static class LoginResponse {
        private String token;
        private String username;
        private String nickname;
        private String role;
    }
}

