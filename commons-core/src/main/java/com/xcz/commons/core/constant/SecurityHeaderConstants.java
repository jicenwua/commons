package com.xcz.commons.core.constant;

/**
 * 安全请求头常量类
 */
public class SecurityHeaderConstants {

    // 加密相关请求头
    public static final String ENCRYPTED_BODY = "X-Encrypted-Body";
    public static final String INITIALIZATION_VECTOR = "X-IV";
    public static final String ENCRYPTED_RESPONSE = "X-Encrypted";

    // 签名验证相关请求头
    public static final String TIMESTAMP = "X-Timestamp";
    public static final String NONCE = "X-Nonce";
    public static final String SIGNATURE = "X-Signature";

}
