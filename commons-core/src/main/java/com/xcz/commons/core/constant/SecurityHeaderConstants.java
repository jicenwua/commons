package com.xcz.commons.core.constant;

/**
 * 安全请求头常量类
 */
public class SecurityHeaderConstants {

    // 加密相关请求头
    /** 是否启用网关加解密（通常为 true）；GET 无 body 时亦会携带 */
    public static final String ENCRYPTED_BODY = "X-Encrypted-Body";
    /** 请求/响应 AES 加密的初始化向量（Base64）；请求由客户端传入，响应由网关写入 */
    public static final String INITIALIZATION_VECTOR = "X-IV";
    /** 响应体是否已加密（通常为 true） */
    public static final String ENCRYPTED_RESPONSE = "X-Encrypted";
    /** RSA 加密后的 AES 会话密钥（POST/PUT/GET 均由请求头传入） */
    public static final String ENCRYPTED_KEY_HEADER = "X-Encrypted-Key";
    /** exchange 属性：本次请求协商的 AES 会话密钥（Base64） */
    public static final String SESSION_AES_KEY_ATTR = "crypto.sessionAesKey";


    // 签名验证相关请求头
    /***客户端请求时间戳（毫秒），网关用于校验请求是否在有效期内，防重放**/
    public static final String TIMESTAMP = "X-Timestamp";
    /***随机数，与时间戳一并参与签名，避免相同参数在有效期内被重复提交**/
    public static final String NONCE = "X-Nonce";
    /***请求签名（SHA-256），由 query 参数、timestamp、nonce 与密钥按约定规则计算**/
    public static final String SIGNATURE = "X-Signature";

}
