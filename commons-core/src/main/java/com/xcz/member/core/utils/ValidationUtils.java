package com.xcz.member.core.utils;

import com.xcz.member.core.exception.ServiceException;

/**
 * 通用参数校验工具。
 */
public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static Long requireShopId(Long shopId) {
        if (shopId == null) {
            throw new ServiceException("店铺 ID 不能为空", 400);
        }
        return shopId;
    }
}
