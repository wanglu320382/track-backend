package com.track.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 查询文本对称加解密工具（前后端共用约定）
 */
@Component
public class StatCryptoUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int AES_128_BYTES = 16;
    private final String aesKey;
    private final String aesIv;

    public StatCryptoUtil(
            @Value("${track.crypto.aes-key:}") String aesKey,
            @Value("${track.crypto.aes-iv:}") String aesIv
    ) {
        this.aesKey = aesKey;
        this.aesIv = aesIv;
    }

    private byte[] require16BytesUtf8(String value, String configKey) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("缺少必要配置：" + configKey);
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != AES_128_BYTES) {
            throw new IllegalStateException(configKey + " 长度必须为 16 字节(UTF-8)");
        }
        return bytes;
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return "";
        }
        try {
            byte[] keyBytes = require16BytesUtf8(aesKey, "track.crypto.aes-key");
            byte[] ivBytes = require16BytesUtf8(aesIv, "track.crypto.aes-iv");

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            // 直接使用标准Base64解码
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 对外不暴露细节
            throw new RuntimeException("解密失败");
        }
    }
}
