package com.xcz.member.core.utils;

import java.math.BigDecimal;

/**
 * BigDecimal 安全取值工具。
 */
public final class BigDecimalUtils {

    private BigDecimalUtils() {
    }

    public static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
