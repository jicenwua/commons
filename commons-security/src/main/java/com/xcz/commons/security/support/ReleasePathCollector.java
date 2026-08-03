package com.xcz.commons.security.support;

import com.xcz.commons.security.annotation.Release;
import com.xcz.commons.security.config.properties.IgnoreProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * 扫描 @Release 注解，收集免认证请求路径。
 */
@Slf4j
public class ReleasePathCollector {

    @Getter
    private final List<String> urls = new CopyOnWriteArrayList<>();

    /**
     * 从 HandlerMapping 扫描结果中提取带 @Release 的路径。
     */
    public void collect(Map<?, HandlerMethod> handlerMethods, Function<Object, Set<String>> patternExtractor) {
        handlerMethods.forEach((mappingInfo, handlerMethod) -> {
            if (!hasRelease(handlerMethod)) {
                return;
            }
            patternExtractor.apply(mappingInfo).forEach(path -> addUrl(path, handlerMethod));
        });
    }

    private void addUrl(String path, HandlerMethod handlerMethod) {
        if (urls.contains(path)) {
            return;
        }
        urls.add(path);
        log.info("Release 免认证路径: {} -> {}.{}",
                path,
                handlerMethod.getBeanType().getSimpleName(),
                handlerMethod.getMethod().getName());
    }

    /**
     * 是否有注解
     * @param handlerMethod 方法元信息
     * @return  是否拥有该注解
     */
    private static boolean hasRelease(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), Release.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), Release.class);
    }

    /**
     * 合并配置白名单与 {@link Release} 扫描结果。
     */
    public static List<String> mergeIgnoreUrls(IgnoreProperties ignoreProperties, ReleasePathCollector collector) {
        List<String> merged = new ArrayList<>();
        if (ignoreProperties != null && ignoreProperties.getUrls() != null) {
            merged.addAll(ignoreProperties.getUrls());
        }
        if (collector != null) {
            merged.addAll(collector.getUrls());
        }
        return merged;
    }
}
