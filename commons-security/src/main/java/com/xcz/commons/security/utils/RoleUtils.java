package com.xcz.commons.security.utils;

import com.xcz.commons.core.exception.auth.NotPermissionException;
import com.xcz.commons.core.exception.auth.NotRoleException;
import com.xcz.commons.security.extend.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Set;

@Slf4j
public final class RoleUtils {

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
