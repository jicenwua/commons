package com.xcz.commons.protobuf.generator;

import com.xcz.commons.protobuf.annotation.ProtobufMessage;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 包扫描工具，查找带 {@link ProtobufMessage} 注解的类。
 */
public final class ClassScanner {

    private ClassScanner() {
    }

    /**
     * 扫描多个包，返回所有 @ProtobufMessage 标记的类（排除接口、匿名类等）。
     */
    public static Set<Class<?>> scanPackages(Collection<String> packages) {
        Set<Class<?>> result = new LinkedHashSet<>();
        for (String pkg : packages) {
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .forPackages(pkg)
                    .setScanners(Scanners.TypesAnnotated));
            result.addAll(reflections.getTypesAnnotatedWith(ProtobufMessage.class));
        }
        return result.stream()
                .filter(ClassScanner::isSupportedType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 过滤不支持生成 proto 的类型 */
    private static boolean isSupportedType(Class<?> type) {
        if (type.isInterface() || type.isAnonymousClass() || type.isLocalClass()) {
            return false;
        }
        // 排除 CGLIB / 代理类
        return !type.getName().contains("$$");
    }
}
