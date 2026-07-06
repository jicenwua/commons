package com.xcz.commons.core.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 加密解密工具类
 */
public class CryptoUtils {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /**
     * AES加密
     *
     * @param data 待加密数据
     * @param key  密钥(Base64编码)
     * @param iv   初始化向量(Base64编码)
     * @return 加密后的数据(Base64编码)
     */
    public static String aesEncrypt(String data, String key, String iv) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.getDecoder().decode(key), AES_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder().decode(iv));

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * AES解密
     *
     * @param encryptedData 加密数据(Base64编码)
     * @param key          密钥(Base64编码)
     * @param iv           初始化向量(Base64编码)
     * @return 解密后的数据
     */
    public static String aesDecrypt(String encryptedData, String key, String iv) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.getDecoder().decode(key), AES_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder().decode(iv));

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * SHA-256哈希
     *
     * @param data 待哈希数据
     * @return 哈希值
     */
    public static String sha256Hash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] hashBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256哈希失败", e);
        }
    }

    /**
     * 生成随机AES密钥
     *
     * @return Base64编码的密钥
     */
    public static String generateAesKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(256); // AES-256
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成AES密钥失败", e);
        }
    }

    /**
     * 生成随机 IV
     *
     * @return Base64编码的IV
     */
    public static String generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }

    /**
     * RSA 解密（用于解密前端传来的 AES 密钥）
     *
     * @param encryptedData 加密数据（Base64编码）
     * @param privateKey RSA 私钥（Base64编码的 PKCS8 格式）
     * @return 解密后的数据
     */
    public static String rsaDecrypt(String encryptedData, String privateKey) {
        try {
            // 解码私钥
            byte[] keyBytes = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey key = keyFactory.generatePrivate(keySpec);

            // 解密
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("RSA解密失败", e);
        }
    }

    /**
     * 生成 RSA 密钥对（用于初始化配置）
     * 注意：仅在生成密钥时使用，不在运行时调用
     *
     * @return 密钥对 Map，包含 publicKey 和 privateKey
     */
    public static java.util.Map<String, String> generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyPairGen.initialize(2048);
            KeyPair keyPair = keyPairGen.generateKeyPair();

            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

            java.util.Map<String, String> keyMap = new java.util.HashMap<>();
            keyMap.put("publicKey", publicKey);
            keyMap.put("privateKey", privateKey);
            return keyMap;
        } catch (Exception e) {
            throw new RuntimeException("生成RSA密钥对失败", e);
        }
    }

    public static void main(String[] args) {
        // 生成并打印 RSA 密钥对（仅用于生成配置，生成后删除或注释）
         java.util.Map<String, String> keys = generateRsaKeyPair();
         System.out.println("公钥: " + keys.get("publicKey"));
         System.out.println("私钥: " + keys.get("privateKey"));

        // 生成 AES 密钥
        System.out.println("AES 密钥: " + generateAesKey());
        System.out.println("随机 IV: " + generateIv());
    }
}

