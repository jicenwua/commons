package com.xcz.commons.protobuf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufField {

    /** 字段编号，未指定时按声明顺序自动分配 */
    int number() default 0;

    /** 是否忽略该字段 */
    boolean ignore() default false;

    /** 覆盖 proto 字段名，默认 camelCase → snake_case */
    String name() default "";

    /** 写入 proto 文件的字段注释（Java 源码注释无法通过反射获取，需显式声明） */
    String comment() default "";
}
