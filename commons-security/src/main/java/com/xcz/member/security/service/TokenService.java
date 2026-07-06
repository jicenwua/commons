package com.xcz.member.security.service;

import com.xcz.member.redis.extend.DatabaseEnum;
import com.xcz.member.redis.utils.RedisUtil;
import com.xcz.member.security.extend.LoginUser;
import com.xcz.member.security.utils.JwtUtils;
import com.xcz.member.security.utils.PermissionUtils;
import lombok.AllArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * token处理器
 */
@AllArgsConstructor
public class TokenService {
    private final JwtUtils jwtUtils;

    //登录信息缓存
    private final String LOGIN_KEY = "login:body:";
    //登录缓存持续时间刷新
    private final String LOGIN_REFRESH_KEY = "login:refresh";
    private final RedissonClient redisson = RedisUtil.getRedisson(DatabaseEnum.DATABASE_0);

    private final Function<String, RBucket<LoginUser>> getBucket = token -> {
        String key = LOGIN_KEY + token;
        return redisson.getBucket(key);
    };


    /**
     * 创建token
     *
     * @param loginUser 登录用户信息
     * @return 创建的token
     */
    public String createToken(LoginUser loginUser) {

        String token = jwtUtils.createToken(loginUser.getUsername(), loginUser.getUserId(), loginUser.getVersion());
        loginUser.setToken(token);
        getBucket.apply(token).set(loginUser, Duration.ofMillis(jwtUtils.getExpiration()));
        return token;
    }

    /**
     * 验证token是否过期
     *
     * @param token    用户token
     * @param username 用户名
     * @return 是否过期
     */
    public String verifyToken(String token, String username) {
        try {
            String userName = jwtUtils.getUserName(token);
            if (userName.equals(username)) {
                return null;
            }
            throw new ArithmeticException("登录失效");
        } catch (Exception e) {
            boolean exists = getBucket.apply(token).isExists();
            //不为空，则说明用户应该继续保持登录状态
            if (exists) {
                return createToken(getLoginUser(token));
            } else {
                throw new ArithmeticException("请先登录");
            }
        }
    }

    /**
     * 检查权限/角色版本，必要时签发新 token（复用已加载的 {@link LoginUser}，避免重复读 Redis）。
     *
     * @return 新 token；无变更时返回 null
     */
    public String checkVersion(String token, LoginUser loginUser) {
        boolean exitsChange = PermissionUtils.isExitsChange(loginUser.getUserId());
        String newToken = null;
        //判断用户角色是否有更新
        if (exitsChange) {
            List<String> roleChange = PermissionUtils.getRoleChange(loginUser.getUserId());
            if (roleChange.isEmpty()) {
                return null;
            }
            //根据用户新角色获取对应的权限进行设置
            Map<String, Set<String>> permissions = PermissionUtils.getRoles();
            // 创建可修改的 Map 副本，避免 UnsupportedOperationException
            Map<String, Set<String>> userPermissions = new HashMap<>(permissions.size());
            roleChange.forEach(role -> {
                userPermissions.put(role, permissions.get(role));
            });
            loginUser.setPermissions(userPermissions);
            PermissionUtils.syncRoleVersions(loginUser, roleChange);
            newToken = createToken(loginUser);
        } else {
            //判断用户角色权限是否有更新
            List<String> changeRoles = jwtUtils.chickVersion(token);
            if (changeRoles != null && !changeRoles.isEmpty()) {
                // 创建可修改的 Map 副本，避免 UnsupportedOperationException
                Map<String, Set<String>> permissions = new HashMap<>(loginUser.getPermissions());
                changeRoles.forEach(role -> {
                    permissions.put(role, PermissionUtils.getPermission(role));
                });
                loginUser.setPermissions(permissions);
                PermissionUtils.syncRoleVersions(loginUser, changeRoles);
                newToken = createToken(loginUser);
            }
        }
        return newToken;
    }

    /**
     * 刷新token有效期
     *
     * @param token 用户token
     */
    public void refreshToken(String token) {
        //如果配置开启了动态刷新才会在登录的时候刷新一次持有登录的时间
        if (jwtUtils.isRefresh()) {
            RSetCache<String> refreshSet = redisson.getSetCache(LOGIN_REFRESH_KEY);
            // 使用add的返回值保证原子性，返回true表示首次添加成功，可以进行刷新，半天保持刷新一次登录token
            boolean isFirstAdd = refreshSet.add(token, 12, TimeUnit.HOURS);
            if (isFirstAdd) {
                getBucket.apply(token).expire(Duration.ofMillis(jwtUtils.getExpiration()));
            }
        }
    }

    /**
     * 登出
     *
     * @param token 用户token
     * @return 等处是否成功
     */
    public boolean logout(String token) {

        redisson.getSetCache(LOGIN_REFRESH_KEY).remove(token);
        return getBucket.apply(token).delete();
    }

    /**
     * 轻量登录校验：不加载 {@link LoginUser}、不续签、不刷新权限版本。
     * 完整认证逻辑见 {@link com.xcz.member.security.support.AuthenticationSessionSupport}。
     */
    public void assertLoginSession(String token) {
        if (token == null || token.isBlank()) {
            throw new ArithmeticException("请先登录");
        }
        try {
            jwtUtils.parseJWT(token);
            return;
        } catch (Exception ignored) {
            // JWT 过期时以 Redis 会话是否存在为准
        }
        if (!getBucket.apply(token).isExists()) {
            throw new ArithmeticException("请重新登录");
        }
    }

    /**
     * 获取登录的用户信息
     *
     * @param token 用户token
     * @return 登录用户信息
     */
    public LoginUser getLoginUser(String token) {

        LoginUser loginUser = getBucket.apply(token).get();
        if (loginUser == null) {
            throw new ArithmeticException("请先登录");
        }
        refreshToken(token);
        return loginUser;
    }

    /**
     * 更新缓存中的用户信息
     *
     * @param loginUser 新信息
     */
    public void updateLoginUser(LoginUser loginUser) {
        getBucket.apply(loginUser.getToken()).set(loginUser);
    }


    public record AuthSession(String token, LoginUser loginUser) {}

    /**
     * 解析请求 token：JWT 有效则加载会话；JWT 过期但 Redis 仍存在则续签。
     * 每个请求至多一次 {@link #getLoginUser(String)}（续签路径为 EXISTS + GET）。
     */
    public AuthSession resolveSession(String requestToken) {
        //先解析token，如果解析失败，则判断是否登录过期
        try {
            jwtUtils.parseJWT(requestToken);
            return new AuthSession(requestToken, getLoginUser(requestToken));
        } catch (Exception e) {
            //过期则提示重新登录，没过期则获取新token
            if (!getBucket.apply(requestToken).isExists()) {
                throw new ArithmeticException("请重新登录");
            }
            LoginUser loginUser = getLoginUser(requestToken);
            String newToken = createToken(loginUser);
            logout(requestToken);
            return new AuthSession(newToken, loginUser);
        }
    }


    public String verifyToken(String token) {
        AuthSession session = resolveSession(token);
        return session.token().equals(token) ? null : session.token();
    }
}
