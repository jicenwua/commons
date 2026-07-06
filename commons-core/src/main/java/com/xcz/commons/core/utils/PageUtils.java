package com.xcz.commons.core.utils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 分页与列表切片工具。
 */
public final class PageUtils {

    private PageUtils() {
    }

    /**
     * 对内存列表进行分页切片。
     */
    public static <T> List<T> slice(List<T> all, int pageNum, int pageSize) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = pageSize < 1 ? 10 : pageSize;
        int total = all.size();
        int from = Math.min((safePageNum - 1) * safePageSize, total);
        int to = Math.min(from + safePageSize, total);
        return all.subList(from, to);
    }

    public static long sliceTotal(List<?> all) {
        return all == null ? 0L : all.size();
    }
}
