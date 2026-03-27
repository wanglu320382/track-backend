package com.track.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 查询文本对称加解密工具（前后端共用约定）
 */
public class StatCryptoUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int AES_128_BYTES = 16;

    private static String requireSecret(String envKey) {
        String v = System.getenv(envKey);
        if (v == null || v.trim().isEmpty()) {
            v = System.getProperty(envKey);
        }
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalStateException("缺少必要配置：" + envKey);
        }
        return v;
    }

    private static byte[] require16BytesUtf8(String envKey) {
        byte[] bytes = requireSecret(envKey).getBytes(StandardCharsets.UTF_8);
        if (bytes.length != AES_128_BYTES) {
            throw new IllegalStateException(envKey + " 长度必须为 16 字节(UTF-8)");
        }
        return bytes;
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return "";
        }
        try {
            byte[] keyBytes = require16BytesUtf8("TRACK_STAT_AES_KEY");
            byte[] ivBytes = require16BytesUtf8("TRACK_STAT_AES_IV");

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 对外不暴露细节
            throw new RuntimeException("解密失败");
        }
    }
}
