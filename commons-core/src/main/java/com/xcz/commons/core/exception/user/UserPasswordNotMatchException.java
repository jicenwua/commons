package com.xcz.commons.core.exception.user;

import com.xcz.commons.core.constant.Constants;

/**
 * 用户密码不正确或不符合规范异常类
 */
public class UserPasswordNotMatchException extends UserException {
    private static final long serialVersionUID = 1L;

    public UserPasswordNotMatchException() {
        super(Constants.FAIL, "账号或密码错误");
    }
}
