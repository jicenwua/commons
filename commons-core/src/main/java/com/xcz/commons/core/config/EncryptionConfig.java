//package com.xcz.common.core.config;
//
//import com.xcz.common.core.utils.CryptoUtils;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * 安全配置类
// */
//@Configuration
//public class EncryptionConfig {
//
//    @Value("${security.encryption.secret-key:defaultSecretKey123456}")
//    private String secretKey;
//
//    /**
//     * 初始化加密密钥
//     */
//    @Bean
//    public String encryptionKey() {
//        // 验证密钥长度
//        if (secretKey.length() < 32) {
//            throw new IllegalArgumentException("加密密钥长度至少需要32位");
//        }
//        return secretKey;
//    }
//
//    /**
//     * 初始化初始化向量
//     */
//    @Bean
//    public String initializationVector() {
//        return CryptoUtils.generateIv();
//    }
//}
