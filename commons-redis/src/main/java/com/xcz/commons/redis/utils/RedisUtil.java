package com.xcz.commons.redis.utils;

import com.xcz.commons.core.utils.SpringUtils;
import com.xcz.commons.redis.extend.DatabaseEnum;
import org.redisson.api.RedissonClient;

public class RedisUtil {

    public static RedissonClient getRedisson(DatabaseEnum databaseEnum){
        return SpringUtils.getBean(databaseEnum.getBeanName());
    }
}
