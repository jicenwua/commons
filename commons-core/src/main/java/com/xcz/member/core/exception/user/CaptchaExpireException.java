package com.xcz.member.core.exception.user;

import com.xcz.member.core.constant.Constants;

/**
 * 验证码失效异常类
 */
public class CaptchaExpireException extends UserException {
    private static final long serialVersionUID = 1L;

    public CaptchaExpireException() {
        super(Constants.FAIL, null);
    }
}
