package com.xcz.commons.protobuf.generator;

import com.xcz.commons.protobuf.annotation.ProtobufMessage;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class ClassScanner {

    private ClassScanner() {
    }

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

    private static boolean isSupportedType(Class<?> type) {
        if (type.isInterface() || type.isAnonymousClass() || type.isLocalClass()) {
            return false;
        }
        return !type.getName().contains("$$");
    }
}
