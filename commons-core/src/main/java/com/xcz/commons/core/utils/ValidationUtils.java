package com.xcz.commons.core.utils;

import com.xcz.commons.core.exception.ServiceException;

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
