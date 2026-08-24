package com.xcz.commons.security.config;


import com.xcz.commons.security.utils.RoleUtils;

/**
 * Spring Security 自定义权限表达式
 */
public class PermissionExpression {

    /**
     * 验证用户是否具备某权限
     */
    public boolean hasPermi(String permission) {
        return RoleUtils.hasPermi(permission);
    }

    /**
     * 验证用户是否不具备某权限
     */
    public boolean lacksPermi(String permission) {
        return RoleUtils.lacksPermi(permission);
    }

    /**
     * 验证用户是否具有以下任意一个权限
     */
    public boolean hasAnyPermi(String permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        String[] permissionArray = permissions.split(",");
        return RoleUtils.hasAnyPermi(permissionArray);
    }

    /**
     * 判断用户是否拥有某个角色
     */
    public boolean hasRole(String role) {
        return RoleUtils.hasRole(role);
    }

    /**
     * 判断用户是否不具备某个角色
     */
    public boolean lacksRole(String role) {
        return RoleUtils.lacksRole(role);
    }

    /**
     * 验证用户是否具有以下任意一个角色
     */
    public boolean hasAnyRole(String roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        String[] roleArray = roles.split(",");
        return RoleUtils.hasAnyRole(roleArray);
    }
}
