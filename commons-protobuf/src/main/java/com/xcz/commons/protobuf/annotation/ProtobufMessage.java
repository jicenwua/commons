package com.xcz.commons.protobuf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要生成 Protobuf 定义的 Java 类型（实体类、枚举）。
 * 扫描包时只会处理带此注解的类。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufMessage {

    /** 覆盖 proto message / enum 名称，默认使用类名 */
    String name() default "";

    /** 覆盖 proto 文件中的注释 */
    String comment() default "";

    /**
     * 路由键：Kafka Topic、RabbitMQ routing key、Netty 消息类型等。
     * 配置后消费者可按路由自动反序列化，无需再写 protobuf.route.mapping。
     */
    String topic() default "";
}
