package com.xcz.member.core.constant;

import java.util.HashSet;
import java.util.Set;

/**
 * 权限相关通用常量
 */
public class SecurityConstants {
    /**
     * 用户ID字段
     */
    public static final String DETAILS_USER_ID = "user_id";

    /**
     * 用户名字段
     */
    public static final String DETAILS_USERNAME = "username";

    /**
     * 授权信息字段
     */
    public static final String AUTHORIZATION_HEADER = "authorization";

    /**
     * 请求来源
     */
    public static final String FROM_SOURCE = "from-source";

    /**
     * 内部请求
     */
    public static final String INNER = "inner";

    /**
     * 用户标识
     */
    public static final String USER_KEY = "user_key";

    /**
     * 服务间 Feign 转发标记（管理后台经 Feign 调用下游时携带，用于跳过店铺角色校验等业务逻辑）
     */
    public static final String FEIGN_INVOKE = "Feign";

    public static final String FEIGN_INVOKE_VALUE = "true";

    /**
     * 服务间 Feign 内部认证头（仅合法 Feign 调用携带，不可由外部伪造）
     */
    public static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service";

    /**
     * 内部服务认证默认值（生产环境应通过 Nacos security.internal-service.token 覆盖）
     */
    public static final String INTERNAL_SERVICE_VALUE = "mobi-feign";

    /**
     * 小程序 C 端用户角色键
     */
    public static final String WX_APP_ROLE = "wx-app";

    /**
     * 登录用户
     */
    public static final String LOGIN_USER = "login_user";

    /**
     * 角色权限
     */
    public static final String ROLE_PERMISSION = "role_permission";

    /**
     * 内部请求白名单
     */
    public static final Set<String> INNER_WHITELIST = new HashSet<>();

    static {
        // 用户认证接口
        INNER_WHITELIST.add("/user/login");
        INNER_WHITELIST.add("/user/register");

        // 测试接口
        INNER_WHITELIST.add("/test/**");

        // 认证相关接口
        INNER_WHITELIST.add("/auth/logout");
        INNER_WHITELIST.add("/auth/refresh");

        // 静态资源和系统接口
        INNER_WHITELIST.add("/favicon.ico");
        INNER_WHITELIST.add("/actuator/**");
        INNER_WHITELIST.add("/v3/api-docs/**");
        INNER_WHITELIST.add("/webjars/**");
        INNER_WHITELIST.add("/swagger-ui/**");
        INNER_WHITELIST.add("/doc.html");
        INNER_WHITELIST.add("/static/**");
    }
}
