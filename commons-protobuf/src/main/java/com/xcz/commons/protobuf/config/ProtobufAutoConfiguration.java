package com.xcz.commons.protobuf.config;

import com.xcz.commons.protobuf.properties.ProtobufProperties;
import com.xcz.commons.protobuf.runtime.ProtobufRuntime;
import com.xcz.commons.protobuf.runtime.ProtobufRuntimeHolder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Protobuf 运行时自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(ProtobufProperties.class)
@ConditionalOnProperty(prefix = "protobuf", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProtobufAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProtobufRuntime protobufRuntime(ProtobufProperties properties) {
        if (properties.getScanPackages().isEmpty() && properties.getScanClasses().isEmpty()) {
            throw new IllegalStateException("请配置 protobuf.scan-packages 或 protobuf.scan-classes");
        }
        ProtobufRuntime.Builder builder = ProtobufRuntime.builder();
        if (!properties.getScanPackages().isEmpty()) {
            builder.scanPackages(properties.getScanPackages());
        }
        if (!properties.getScanClasses().isEmpty()) {
            builder.scanClasses(properties.getScanClasses());
        }
        properties.getRouteMapping().forEach(builder::route);
        ProtobufRuntime runtime = builder.build();
        ProtobufRuntimeHolder.bind(runtime);
        return runtime;
    }
}
