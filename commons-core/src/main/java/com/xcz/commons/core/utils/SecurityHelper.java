package com.xcz.commons.core.utils;

import com.xcz.commons.core.constant.SecurityHeaderConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * 安全助手类 - 提供请求加密和签名的便捷方法
 */
public class SecurityHelper {

    /**
     * 生成加密请求头
     *
     * @param params  请求参数
     * @param secret  密钥
     * @return 包含安全头信息的Map
     */
    public static Map<String, String> generateSecureHeaders(Map<String, String> params, String secret) {
        Map<String, String> headers = new HashMap<>();

        // 生成时间戳和随机数
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = String.valueOf(System.currentTimeMillis() % 1000000);

        // 生成签名
        String signature = RequestSignatureUtils.generateSignature(params, timestamp, nonce, secret);

        // 设置安全头
        headers.put(SecurityHeaderConstants.TIMESTAMP, timestamp);
        headers.put(SecurityHeaderConstants.NONCE, nonce);
        headers.put(SecurityHeaderConstants.SIGNATURE, signature);

        return headers;
    }

    /**
     * 生成加密数据
     *
     * @param data   原始数据
     * @param secret 密钥
     * @return 包含加密数据和IV的对象
     */
    public static EncryptedData encryptData(String data, String secret) {
        String iv = CryptoUtils.generateIv();
        String encrypted = CryptoUtils.aesEncrypt(data, secret, iv);
        return new EncryptedData(encrypted, iv);
    }

    /**
     * 解密数据
     *
     * @param encryptedData 加密数据
     * @param iv            初始化向量
     * @param secret        密钥
     * @return 解密后的数据
     */
    public static String decryptData(String encryptedData, String iv, String secret) {
        return CryptoUtils.aesDecrypt(encryptedData, secret, iv);
    }

    /**
     * 解密响应数据
     *
     * @param encryptedResponse 加密的响应数据
     * @param responseHeaders 响应头信息
     * @param secret 密钥
     * @return 解密后的响应数据
     */
    public static String decryptResponse(String encryptedResponse, Map<String, String> responseHeaders, String secret) {
        String iv = responseHeaders.get(SecurityHeaderConstants.INITIALIZATION_VECTOR);
        if (iv == null || iv.isEmpty()) {
            throw new IllegalArgumentException("响应中缺少" + SecurityHeaderConstants.INITIALIZATION_VECTOR + "头信息");
        }
        return CryptoUtils.aesDecrypt(encryptedResponse, secret, iv);
    }

    /**
     * 加密数据封装类
     */
    public static class EncryptedData {
        private final String encryptedData;
        private final String iv;

        public EncryptedData(String encryptedData, String iv) {
            this.encryptedData = encryptedData;
            this.iv = iv;
        }

        public String getEncryptedData() {
            return encryptedData;
        }

        public String getIv() {
            return iv;
        }
    }
}
