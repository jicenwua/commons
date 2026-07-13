package com.xcz.commons.security.extend;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xcz.commons.core.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /***用户ID**/
    private Long userId;
    /***用户唯一标识**/
    private String token;
    /***登录IP地址**/
    private String ipaddr;
    /***登录地点**/
    private String loginLocation;
    /***权限列表**/
    private Map<String,Set<String>> permissions;
    /***用户账号**/
    private String username;
    /***用户名**/
    private String name;
    /***用户头像**/
    private String avatar;
    /***版本号**/
    private Map<String,Long> version;
    /***密码**/
    @JsonIgnore
    private String password;
    /***账户状态**/
    private boolean enabled;
    /***账户锁定状态**/
    private boolean accountNonLocked;
    /***凭证过期状态**/
    private boolean credentialsNonExpired;
    /***账户过期状态**/
    private boolean accountNonExpired;

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 账户是否未过期
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    /**
     * 账户是否未锁定
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    /**
     * 凭证是否未过期
     */
    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    /**
     * 账户是否启用
     */
    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<String> aUthorityList = getAUthorityList();
        // 将权限字符串转换为GrantedAuthority对象
        return aUthorityList.stream()
                .filter(StringUtils::isNotEmpty)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * 检测角色权限版本
     * @return  需要更新的权限版本
     */
    public List<String> getAUthorityList() {
        List<String> authorities = new ArrayList<>();
        if (permissions != null && !permissions.isEmpty()) {
            Set<String> roles = permissions.keySet();
            Collection<Set<String>> values = permissions.values();
            Set<String> uniquePermissions = values.stream()
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            authorities.addAll( roles);
            authorities.addAll(uniquePermissions);
        }
        return authorities;
    }


    /**
     * 获取登录用户的权限列表
     * @return set权限列表
     */
    public Set<String> getPermissionSet(){
        return this.getPermissions().values()
                .stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 获取用户的角色列表
     * @return  set角色列表
     */
    public Set<String> getRoleSet(){
        return this.getPermissions().keySet();
    }
}
