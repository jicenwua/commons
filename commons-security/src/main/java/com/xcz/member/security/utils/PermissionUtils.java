package com.xcz.member.security.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.member.core.constant.TokenConstants;
import com.xcz.member.core.exception.auth.NotPermissionException;
import com.xcz.member.core.exception.auth.NotRoleException;
import com.xcz.member.core.utils.StringUtils;
import com.xcz.member.redis.extend.DatabaseEnum;
import com.xcz.member.redis.utils.RedisUtil;
import com.xcz.member.security.extend.LoginUser;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 权限验证工具类
 * <p>
 * 对登录用户权限进行缓存更新，从用户角色变更到用户角色权限的变更两个角度进行动态更新用户权限并生成新的token和权限
 * 返回给前段进行操作
 * </p>
 */
@Slf4j
public class PermissionUtils {

    /*** 版本缓存（缓存角色版本，要是有版本变更则获取缓存的新权限进行更新） ***/
    private static final Map<String, Long> versionMap = new ConcurrentHashMap<>();
    /*** 缓存角色对应的权限列表（只是角色的权限变了） **/
    private static final String SYS_PERMISSION = "sys:role:permission";
    /*** 用户新角色缓存（用户的角色变了） **/
    private static final String SYS_USER_ROLE_CHANGE = "sys:role:user:";
    /**
     * 角色变更列表专用序列化（纯 JSON 数组），避免 Redisson JsonJacksonCodec 将 roleKey（如 admin）误判为多态类型 id
     */
    private static final ObjectMapper ROLE_CHANGE_JSON = new ObjectMapper();
    private static final TypeReference<List<String>> ROLE_CHANGE_TYPE = new TypeReference<>() {
    };
    private static final RedissonClient redisson = RedisUtil.getRedisson(DatabaseEnum.DATABASE_0);


    /**
     * 启动时构建权限变更通知订阅监听
     */
    @PostConstruct
    public void init() {
        //初始化创建版本更新监听，保存版本修改到内存呢中以便动态修改
        RTopic topic = RedisUtil.getRedisson(DatabaseEnum.DATABASE_0).getTopic(TokenConstants.VERSION_TOPIC);
        topic.addListener(String.class, (channel, jsonMessage) -> {
            try {
                log.info("收到版本更新通知, channel: {}, message: {}", channel, jsonMessage);

                if (jsonMessage != null) {
                    Map<String, Long> message = ROLE_CHANGE_JSON.readValue(jsonMessage, new TypeReference<Map<String, Long>>() {
                    });
                    versionMap.putAll(message);
                }
            } catch (Exception e) {
                log.error("处理版本更新消息失败", e);
            }
        });
    }


    /**
     * 广播角色权限版本变更，各节点更新 {@link #versionMap}，供 JWT {@code chickVersion} 检测
     */
    public static void publishRoleVersion(String roleKey, Long version) {
        if (StringUtils.isEmpty(roleKey) || version == null) {
            return;
        }
        try {
            //构建普通的 HashMap
            Map<String, Long> messageMap = new HashMap<>();
            messageMap.put(roleKey, version);

            //手动转成普通的 JSON 字符串
            String jsonMessage = ROLE_CHANGE_JSON.writeValueAsString(messageMap);

            //显式指定 StringCodec 发送
            redisson.getTopic(TokenConstants.VERSION_TOPIC)
                    .publish(jsonMessage);

        } catch (Exception e) {
            log.error("广播角色权限版本变更失败", e);
        }
    }

    /**
     * 根据权限标识获取版本号
     *
     * @param roleKey 权限标识
     * @return 版本号
     */
    public static Long getVersion(String roleKey) {
        return versionMap.get(roleKey);
    }

    /**
     * 判断版本缓存是否为空
     *
     * @return true-缓存为空, false-缓存不为空
     */
    public static boolean isEmpty() {
        return versionMap.isEmpty();
    }

    /**
     * 判断版本缓存是否包含指定角色键
     *
     * @param roleKey 角色键
     * @return true-包含该角色键, false-不包含
     */
    public static boolean containsKey(String roleKey) {
        return versionMap.containsKey(roleKey);
    }

    /**
     * 设置角色权限缓存
     *
     * @param permission 角色权限map
     */
    public static void setRoleCache(Map<String, Set<String>> permission) {
        if (!CollectionUtils.isEmpty(permission)) {
            redisson.getMap(SYS_PERMISSION).putAll(permission);
        }
    }

    /**
     * 获取所有角色权限
     *
     * @return 所有角色权限
     */
    public static Map<String, Set<String>> getRoles() {
        return redisson.getMap(SYS_PERMISSION);
    }

    /**
     * 获取单个角色权限
     *
     * @param role 角色名
     * @return 角色权限
     */
    public static Set<String> getPermission(String role) {
        RMap<String, Set<String>> map = redisson.getMap(SYS_PERMISSION);
        return map.get(role);
    }

    /**
     * 删除角色权限缓存
     */
    public static void deletePermission() {
        redisson.getMap(SYS_PERMISSION).unlink();
    }

    /**
     * 从全局角色权限缓存中移除指定 roleKey（直接作用于 Redis Hash）
     */
    public static void removeRolePermission(String roleKey) {
        if (StringUtils.isNotEmpty(roleKey)) {
            redisson.getMap(SYS_PERMISSION).remove(roleKey);
        }
    }

    /**
     * 使用 StringCodec 存裸 JSON，避免 JsonJacksonCodec 对 String 二次编码导致删不干净
     */
    private static RBucket<String> roleChangeBucket(Long userId) {
        return redisson.getBucket(SYS_USER_ROLE_CHANGE + userId, StringCodec.INSTANCE);
    }


    /**
     * 添加角色权限变更
     *
     * @param userId 用户id
     * @param roles  变更列表
     */
    public static void addRoleChange(Long userId, List<String> roles) {
        if (userId == null) {
            return;
        }
        try {
            String json = ROLE_CHANGE_JSON.writeValueAsString(roles != null ? roles : Collections.emptyList());
            roleChangeBucket(userId).set(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化角色变更列表失败, userId=" + userId, e);
        }
    }


    /**
     * 判断是否有修改
     *
     * @param userId 用户id
     * @return 是否有修改
     */
    public static boolean isExitsChange(Long userId) {
        if (userId == null) {
            return false;
        }
        return roleChangeBucket(userId).isExists();
    }

    /**
     * 获取用户新变更角色
     *
     * @param userId 用户id
     * @return 角色列表
     */
    public static List<String> getRoleChange(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        RBucket<String> bucket = roleChangeBucket(userId);
        try {
            if (!bucket.isExists()) {
                return Collections.emptyList();
            }
            String json = bucket.get();
            if (StringUtils.isEmpty(json)) {
                return Collections.emptyList();
            }
            List<String> roles = ROLE_CHANGE_JSON.readValue(json, ROLE_CHANGE_TYPE);
            return roles != null ? roles : Collections.emptyList();
        } catch (Exception e) {
            log.warn("解析角色变更缓存失败, userId={}", userId, e);
            return Collections.emptyList();
        } finally {
            // 消费后必须删除标记，避免每次请求都触发 checkVersion 刷新
            bucket.unlink();
        }
    }


    /**
     * 签发新 token 前，将 JWT 中的角色版本号与内存 {@link #versionMap} 对齐
     */
    public static void syncRoleVersions(LoginUser loginUser, Collection<String> roles) {
        if (loginUser == null || CollectionUtils.isEmpty(roles)) {
            return;
        }
        Map<String, Long> version = loginUser.getVersion();
        //如果为空则设置新的map
        if (version == null) {
            version = new HashMap<>();
            loginUser.setVersion(version);
        }
        //根据需要设置版本的权限获取内存中的最新版本，要是没有则设置为0
        for (String role : roles) {
            Long current = versionMap.get(role);
            //如果有该角色的新权限版本则设置为版本，否则蛇者为0版本
            if (current != null) {
                version.put(role, current);
            } else {
                version.put(role, 0L);
            }
        }
    }


    //================================== 注解权限验证 ==========================================
    private static LoginUser verify(String... permission) {
        if (permission == null || permission.length == 0) {
            return null;
        }

        return SecurityUtils.getLoginUser();
    }

    /**
     * 验证用户是否具备某权限
     *
     * @param permission 权限标识
     * @return 用户是否具备某权限
     */
    public static boolean hasPermi(String permission) {
        LoginUser loginUser = verify(permission);
        if (loginUser == null) {
            return false;
        }

        // 超级管理员拥有所有权限
        if (isAdmin(loginUser)) {
            return true;
        }

        Set<String> permissions = loginUser.getPermissionSet();
        return !CollectionUtils.isEmpty(permissions) && permissions.contains(permission);
    }


    /**
     * 验证用户是否不具备某权限
     *
     * @param permission 权限标识
     * @return 用户是否不具备某权限
     */
    public static boolean lacksPermi(String permission) {
        return !hasPermi(permission);
    }

    /**
     * 验证用户是否具有以下任意一个权限
     *
     * @param permissions 权限列表
     * @return 用户是否具有以下任意一个权限
     */
    public static boolean hasAnyPermi(String... permissions) {
        LoginUser loginUser = verify(permissions);
        if (loginUser == null) {
            return false;
        }

        // 超级管理员拥有所有权限
        if (isAdmin(loginUser)) {
            return true;
        }


        Set<String> userPermissions = loginUser.getPermissionSet();
        if (CollectionUtils.isEmpty(userPermissions)) {
            return false;
        }

        for (String permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断用户是否拥有某个角色
     *
     * @param role 角色标识
     * @return 用户是否具备某角色
     */
    public static boolean hasRole(String role) {
        LoginUser loginUser = verify(role);
        if (loginUser == null) {
            return false;
        }

        // 超级管理员拥有所有权限
        if (isAdmin(loginUser)) {
            return true;
        }

        Set<String> roles = loginUser.getRoleSet();
        return !CollectionUtils.isEmpty(roles) && roles.contains(role);
    }

    /**
     * 判断用户是否不具备某个角色
     *
     * @param role 角色标识
     * @return 用户是否不具备某角色
     */
    public static boolean lacksRole(String role) {
        return !hasRole(role);
    }

    /**
     * 验证用户是否具有以下任意一个角色
     *
     * @param roles 角色列表
     * @return 用户是否具有以下任意一个角色
     */
    public static boolean hasAnyRole(String... roles) {
        LoginUser loginUser = verify(roles);
        if (loginUser == null) {
            return false;
        }

        // 超级管理员拥有所有权限
        if (isAdmin(loginUser)) {
            return true;
        }

        Set<String> userRoles = loginUser.getRoleSet();
        if (CollectionUtils.isEmpty(userRoles)) {
            return false;
        }

        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否为管理员
     *
     * @param loginUser 登录用户
     * @return 是否为管理员
     */
    private static boolean isAdmin(LoginUser loginUser) {
        if (loginUser == null) {
            return false;
        }
        Set<String> roles = loginUser.getRoleSet();
        return !CollectionUtils.isEmpty(roles) && roles.contains("admin");
    }

    /**
     * 校验权限，如果没有权限则抛出异常
     *
     * @param permission 权限标识
     */
    public static void checkPermission(String permission) {
        if (!hasPermi(permission)) {
            throw new NotPermissionException(permission);
        }
    }

    /**
     * 校验角色，如果没有角色则抛出异常
     *
     * @param role 角色标识
     */
    public static void checkRole(String role) {
        if (!hasRole(role)) {
            throw new NotRoleException(role);
        }
    }

}
