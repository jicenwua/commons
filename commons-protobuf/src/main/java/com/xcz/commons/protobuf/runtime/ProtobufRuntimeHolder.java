package com.xcz.commons.protobuf.runtime;

import java.util.Map;

/**
 * 供中间件适配层（如 Kafka Serializer）获取 Spring 托管的 {@link ProtobufRuntime}。
 */
public final class ProtobufRuntimeHolder {

    private static volatile ProtobufRuntime runtime;

    private ProtobufRuntimeHolder() {
    }

    /**
     * 将 Spring 托管的 Runtime 绑定到全局 Holder，供 Kafka Serializer 等非 Spring 组件使用。
     */
    public static void bind(ProtobufRuntime protobufRuntime) {
        runtime = protobufRuntime;
    }

    /**
     * 获取已绑定的 Runtime，未初始化时抛出异常。
     */
    public static ProtobufRuntime require() {
        ProtobufRuntime current = runtime;
        if (current == null) {
            throw new IllegalStateException(
                    "ProtobufRuntime 未初始化，请配置 protobuf.scan-packages 或调用 ProtobufRuntimeHolder.bind()");
        }
        return current;
    }

    /**
     * 优先返回已绑定的 Runtime；否则根据 Kafka 等中间件传入的 configs 临时创建。
     */
    public static ProtobufRuntime getOrCreate(Map<String, ?> configs) {
        ProtobufRuntime current = runtime;
        if (current != null) {
            return current;
        }
        return ProtobufRuntime.fromConfig(configs);
    }
}
