package com.xcz.commons.security.utils;

import com.xcz.commons.core.constant.TokenConstants;
import com.xcz.commons.core.text.Convert;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Jwt工具类
 */
@Data
@Slf4j
@RefreshScope
@ConfigurationProperties(prefix = "security.jwt")
public class JwtUtils {

    /*** JWT密钥 ***/
    private String secret;

    /*** 过期时间（毫秒） ***/
    private Long expiration;

    /*** 是否动态续期令牌 ***/
    private boolean isRefresh = false;


    /**
     * 判断是否有需要更改的角色权限
     *
     * @param token 用户token
     * @return 需要更新的角色权限
     */
    public List<String> chickVersion(String token) {
        List<String> changeRoles = new ArrayList<>();
        //如果没有新版本更新则跳过
        if (!PermissionUtils.isEmpty()) {
            Map<String, Long> userVersion = getUserVersion(token);
            //获取用户权限版本之后，和内存中有最新版本的权限进行对比
            userVersion.forEach((role, version) -> {
                //根据内存中的角色的版本号来对比是否需要更新
                if (PermissionUtils.containsKey(role)) {
                    Long localVersion = PermissionUtils.getVersion(role);
                    if (localVersion != null && localVersion > version) {
                        changeRoles.add(role);
                    }
                }
            });
        }
        return changeRoles;
    }

    /***
     * 创建JWT令牌
     *
     * @param subject 主题
     * @return 令牌
     */
    public String createJWT(String subject) {
        SecureDigestAlgorithm<SecretKey, SecretKey> algorithm = Jwts.SIG.HS512;
        SecretKey key = getSecretKey();

        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + expiration; // 使用动态过期时间

        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(expMillis))
                .signWith(key, algorithm)
                .compact();
    }

    /***
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    public String createToken(Map<String, Object> claims) {
        SecureDigestAlgorithm<SecretKey, SecretKey> algorithm = Jwts.SIG.HS512;
        SecretKey key = getSecretKey();

        return Jwts.builder()
                .claims(claims)
                .signWith(key, algorithm)
                .compact();
    }

    /***
     * 创建用户认证Token
     *
     * @param username  用户名
     * @param userId    用户id
     * @param version   用户权限
     * @return 令牌
     */
    public String createToken(String username, long userId, Map<String, Long> version) {
        SecureDigestAlgorithm<SecretKey, SecretKey> algorithm = Jwts.SIG.HS512;
        SecretKey key = getSecretKey();

        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + expiration; // 使用动态过期时间

        return Jwts.builder()
                .subject(username)
                .claim(TokenConstants.DETAILS_USERNAME, username)
                .claim(TokenConstants.DETAILS_USER_ID, userId)
                .claim(TokenConstants.DETAILS_USER_VERSION, version)
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(expMillis))
                .signWith(key, algorithm)
                .compact();
    }

    /***
     * 解析JWT令牌
     *
     * @param token 令牌
     * @return 数据声明
     */
    public Claims parseJWT(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean verify(String token) {
        try {
            parseJWT(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /***
     * 根据令牌获取用户ID
     *
     * @param token 令牌
     * @return 用户ID
     */
    public String getUserId(String token) {
        Claims claims = parseJWT(token);
        return getValue(claims, TokenConstants.DETAILS_USER_ID);
    }

    /***
     * 根据身份信息获取用户ID
     *
     * @param claims 身份信息
     * @return 用户ID
     */
    public String getUserId(Claims claims) {
        return getValue(claims, TokenConstants.DETAILS_USER_ID);
    }

    /***
     * 根据令牌获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUserName(String token) {
        Claims claims = parseJWT(token);
        return getValue(claims, TokenConstants.DETAILS_USERNAME);
    }

    /***
     * 根据身份信息获取用户名
     *
     * @param claims 身份信息
     * @return 用户名
     */
    public String getUserName(Claims claims) {
        return getValue(claims, TokenConstants.DETAILS_USERNAME);
    }

    /**
     * 获取用户版本
     *
     * @param token 令牌
     * @return 用户权限map
     */
    public Map<String, Long> getUserVersion(String token) {
        Claims claims = parseJWT(token);
        Object versionObj = claims.get(TokenConstants.DETAILS_USER_VERSION);
        Map<String, Long> roleVersionMap = new HashMap<>();
        if (versionObj instanceof Map<?, ?> rawMap) {
            rawMap.forEach((key, value) -> {
                String roleName = String.valueOf(key);
                Long version = Convert.toLong(value, 0L);
                roleVersionMap.put(roleName, version);
            });
        }
        return roleVersionMap;
    }


    /***
     * 根据身份信息获取键值
     *
     * @param claims 身份信息
     * @param key    键
     * @return 值
     */
    public String getValue(Claims claims, String key) {
        return Convert.toStr(claims.get(key), "");
    }

    /***
     * 获取加密用的 SecretKey
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
