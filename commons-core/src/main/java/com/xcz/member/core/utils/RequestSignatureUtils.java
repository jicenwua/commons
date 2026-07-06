package com.xcz.member.core.utils;

import java.util.Map;
import java.util.TreeMap;

/**
 * 请求签名工具类
 */
public class RequestSignatureUtils {

    /**
     * 生成请求签名
     *
     * @param params  请求参数
     * @param timestamp 时间戳
     * @param nonce   随机数
     * @param secret  密钥
     * @return 签名字符串
     */
    public static String generateSignature(Map<String, String> params, String timestamp, String nonce, String secret) {
        // 按参数名排序
        TreeMap<String, String> sortedParams = new TreeMap<>(params);

        // 拼接参数字符串
        StringBuilder paramString = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            paramString.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }

        // 拼接完整待签名字符串
        String signSource = paramString.toString() +
                "timestamp=" + timestamp +
                "&nonce=" + nonce +
                "&secret=" + secret;

        // 生成签名
        return CryptoUtils.sha256Hash(signSource);
    }

    /**
     * 验证请求签名
     *
     * @param params     请求参数
     * @param timestamp  时间戳
     * @param nonce      随机数
     * @param signature  客户端签名
     * @param secret     密钥
     * @return 验证结果
     */
    public static boolean verifySignature(Map<String, String> params, String timestamp, String nonce, String signature, String secret) {
        String calculatedSignature = generateSignature(params, timestamp, nonce, secret);
        return calculatedSignature.equals(signature);
    }
}
