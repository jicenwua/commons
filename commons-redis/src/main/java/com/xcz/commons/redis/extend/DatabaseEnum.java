package com.xcz.commons.redis.extend;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DatabaseEnum {
    DATABASE_0(0, DatabaseBeanName.DATABASE_BEAN_NAME0),
    DATABASE_1(1, DatabaseBeanName.DATABASE_BEAN_NAME1),
    DATABASE_2(2,DatabaseBeanName.DATABASE_BEAN_NAME2),
    DATABASE_3(3,DatabaseBeanName.DATABASE_BEAN_NAME3),
    DATABASE_4(4,DatabaseBeanName.DATABASE_BEAN_NAME4),
    DATABASE_5(5,DatabaseBeanName.DATABASE_BEAN_NAME5),
    DATABASE_6(6,DatabaseBeanName.DATABASE_BEAN_NAME6),
    DATABASE_7(7,DatabaseBeanName.DATABASE_BEAN_NAME7),
    DATABASE_8(8,DatabaseBeanName.DATABASE_BEAN_NAME8),
    DATABASE_9(9,DatabaseBeanName.DATABASE_BEAN_NAME9),
    DATABASE_10(10, DatabaseBeanName.DATABASE_BEAN_NAME10),
    DATABASE_11(11, DatabaseBeanName.DATABASE_BEAN_NAME11),
    DATABASE_12(12, DatabaseBeanName.DATABASE_BEAN_NAME12),
    DATABASE_13(13, DatabaseBeanName.DATABASE_BEAN_NAME13),
    DATABASE_14(14, DatabaseBeanName.DATABASE_BEAN_NAME14),
    DATABASE_15(15, DatabaseBeanName.DATABASE_BEAN_NAME15),
    ;


    private final Integer database;
    private final String beanName;

}
