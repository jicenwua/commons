package com.xcz.member.core.constant;

/**
 * Token的Key常量
 */
public class TokenConstants {
    /**
     * 令牌自定义标识
     */
    public static final String AUTHENTICATION = "Authorization";

    /**
     * 令牌前缀
     */
    public static final String PREFIX = "Bearer ";

    /**
     * 用户ID
     */
    public static final String DETAILS_USER_ID = "user_id";

    /**
     * 用户名
     */
    public static final String DETAILS_USERNAME = "username";

    /**
     * 用户类型
     */
    public static final String DETAILS_USER_TYPE = "user_type";

    /**
     * 用户版本
     */
    public static final String DETAILS_USER_VERSION = "user_version";

    /**
     * 权限变更订阅
     */
    public static final String VERSION_TOPIC = "version:topic";


    /**
     * 用户权限列表
     */
    public static final String CLAIMS_AUTHORITIES = "authorities";
    /**
     * 过期时间字段
     */
    public static final String EXP = "exp";

    /**
     * 发行时间字段
     */
    public static final String IAT = "iat";

}
