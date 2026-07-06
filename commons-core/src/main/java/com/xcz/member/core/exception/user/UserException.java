package com.xcz.member.core.exception.user;

import com.xcz.member.core.exception.base.BaseException;

/**
 * 用户信息异常类
 */
public class UserException extends BaseException {
    private static final long serialVersionUID = 1L;

    public UserException(int code, String message) {
        super("user", code, null, message);
    }

}
