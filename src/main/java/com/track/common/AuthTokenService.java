package com.track.common;

import com.track.entity.SysUser;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的内存令牌管理
 */
@Component
public class AuthTokenService {

    private final Map<String, SysUser> tokenStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成并缓存登录令牌
     */
    public String generateToken(SysUser user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenStore.put(token, user);
        return token;
    }

    /**
     * 根据令牌获取用户
     */
    public SysUser getUserByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return tokenStore.get(token);
    }

    /**
     * 移除令牌
     */
    public void removeToken(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }
}

