package com.mogu.data.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密解密工具类
 *
 * 使用 AES/CBC/PKCS5Padding 算法进行加密和解密
 * 密钥从系统环境变量或配置文件中读取
 *
 * @author MModelX Team
 * @since 2026-05-21
 */
@Component
@Slf4j
public class EncryptionUtil {

    /**
     * 加密算法
     */
    private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * 密钥长度（256位）
     */
    private static final int KEY_LENGTH = 256;

    /**
     * 初始化向量长度（128位）
     */
    private static final int IV_LENGTH = 16;

    /**
     * 密钥（从环境变量或使用默认值）
     * 生产环境应该从环境变量 MMODELX_ENCRYPTION_KEY 读取
     */
    private static final String SECRET_KEY;

    static {
        String key = System.getenv("MMODELX_ENCRYPTION_KEY");
        if (key == null || key.isEmpty()) {
            // 开发环境使用默认密钥
            key = "mmodelx-default-secret-key-for-aes-256-encryption!";
            log.warn("Using default encryption key. Please set MMODELX_ENCRYPTION_KEY environment variable in production.");
        }
        SECRET_KEY = key;
    }

    /**
     * 加密字符串
     *
     * @param plainText 明文
     * @return 加密后的字符串（Base64编码）
     * @throws Exception 如果加密失败
     */
    public static String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // 1. 生成随机 IV（初始化向量）
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // 2. 从密钥生成密钥规范
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            // 确保密钥长度为256位（32字节）
            byte[] keyBytes256 = new byte[32];
            System.arraycopy(keyBytes, 0, keyBytes256, 0, Math.min(keyBytes.length, 32));
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes256, "AES");

            // 3. 创建密码器
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            // 4. 加密
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 5. 组合 IV 和密文
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            byte[] combined = byteBuffer.array();

            // 6. Base64 编码
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new Exception("Failed to encrypt data", e);
        }
    }

    /**
     * 解密字符串
     *
     * @param encryptedText 加密后的字符串（Base64编码）
     * @return 解密后的明文
     * @throws Exception 如果解密失败
     */
    public static String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // 1. Base64 解码
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            if (combined.length < IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted text: too short");
            }

            // 2. 提取 IV 和密文
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[combined.length - IV_LENGTH];

            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, combined.length - IV_LENGTH);

            // 3. 从密钥生成密钥规范
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            // 确保密钥长度为256位（32字节）
            byte[] keyBytes256 = new byte[32];
            System.arraycopy(keyBytes, 0, keyBytes256, 0, Math.min(keyBytes.length, 32));
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes256, "AES");

            // 4. 创建密码器
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            // 5. 解密
            byte[] plainText = cipher.doFinal(cipherText);

            // 6. 转换为字符串
            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new Exception("Failed to decrypt data", e);
        }
    }

    /**
     * 验证密码是否正确
     *
     * @param encryptedText 加密后的密码
     * @param plainText 明文密码
     * @return 是否匹配
     */
    public static boolean verifyPassword(String encryptedText, String plainText) {
        try {
            String decrypted = decrypt(encryptedText);
            return decrypted.equals(plainText);
        } catch (Exception e) {
            log.error("Password verification failed", e);
            return false;
        }
    }

    /**
     * 生成随机盐值
     *
     * @return Base64 编码的盐值
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 生成随机密钥
     *
     * @param length 密钥长度（字节）
     * @return Base64 编码的密钥
     */
    public static String generateKey(int length) {
        byte[] key = new byte[length];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /**
     * SHA-256 哈希
     *
     * @param input 输入字符串
     * @return 哈希值（十六进制字符串）
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("SHA-256 hashing failed", e);
            throw new RuntimeException("Failed to hash data", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
