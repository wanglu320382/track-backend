package com.track.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SQL 文本对称加解密工具（前后端共用约定）
 */
public class SqlCryptoUtil {

    // TODO: 实际项目中请通过配置注入，而不要写死（AES-128 要求 key/IV 均为 16 字节，此处为 16 字符）
    private static final String KEY = "track-backend-sq";   // 16 bytes
    private static final String IV  = "track-backend-iv";  // 16 bytes

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return "";
        }
        try {
            byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
            byte[] ivBytes = IV.getBytes(StandardCharsets.UTF_8);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密 SQL 失败", e);
        }
    }
}

