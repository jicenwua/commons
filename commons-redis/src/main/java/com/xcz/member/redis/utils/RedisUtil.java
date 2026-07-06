package com.xcz.member.redis.utils;

import com.xcz.member.core.utils.SpringUtils;
import com.xcz.member.redis.extend.DatabaseEnum;
import org.redisson.api.RedissonClient;

public class RedisUtil {

    public static RedissonClient getRedisson(DatabaseEnum databaseEnum){
        return SpringUtils.getBean(databaseEnum.getBeanName());
    }
}
