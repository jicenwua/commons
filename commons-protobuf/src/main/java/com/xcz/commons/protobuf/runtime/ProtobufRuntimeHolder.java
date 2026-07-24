package com.xcz.commons.protobuf.runtime;

import java.util.Map;

/**
 * 供中间件适配层（如 Kafka Serializer）获取 Spring 托管的 {@link ProtobufRuntime}。
 */
public final class ProtobufRuntimeHolder {

    private static volatile ProtobufRuntime runtime;

    private ProtobufRuntimeHolder() {
    }

    public static void bind(ProtobufRuntime protobufRuntime) {
        runtime = protobufRuntime;
    }

    public static ProtobufRuntime require() {
        ProtobufRuntime current = runtime;
        if (current == null) {
            throw new IllegalStateException(
                    "ProtobufRuntime 未初始化，请配置 protobuf.scan-packages 或调用 ProtobufRuntimeHolder.bind()");
        }
        return current;
    }

    public static ProtobufRuntime getOrCreate(Map<String, ?> configs) {
        ProtobufRuntime current = runtime;
        if (current != null) {
            return current;
        }
        return ProtobufRuntime.fromConfig(configs);
    }
}
