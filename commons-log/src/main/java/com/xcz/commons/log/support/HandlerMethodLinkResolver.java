package com.xcz.commons.log.support;

import org.springframework.asm.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析 HandlerMethod，生成 IDE 可点击的短格式跳转链接。
 */
public final class HandlerMethodLinkResolver {

    /**
     * 方法首行行号缓存，key = 全限定类名#方法描述符
     */
    private static final ConcurrentHashMap<String, Integer> LINE_CACHE = new ConcurrentHashMap<>();

    private HandlerMethodLinkResolver() {
    }

    /**
     * Controller 方法跳转信息。
     *
     * @param navigationLink 短格式 IDE 链接；非 Controller 时为 {@code handler.toString()}
     */
    public record ControllerMethodLink(String navigationLink) {
    }

    /**
     * 从拦截器 handler 解析跳转链接。
     *
     * @param handler {@link HandlerInterceptor} 传入的 handler
     * @return 解析结果
     */
    public static ControllerMethodLink resolve(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return new ControllerMethodLink(String.valueOf(handler));
        }
        Class<?> beanType = handlerMethod.getBeanType();
        Method method = handlerMethod.getMethod();
        String simpleClassName = beanType.getSimpleName();
        String methodName = method.getName();

        int line = resolveLineNumber(beanType, method);
        String fileRef = line > 0 ? simpleClassName + ".java:" + line : simpleClassName + ".java";
        String navigationLink = simpleClassName + "." + methodName + "(" + fileRef + ")";

        return new ControllerMethodLink(navigationLink);
    }

    /**
     * 读取方法首行源码行号（带缓存）。
     *
     * @return 行号；失败返回 {@code -1}
     */
    private static int resolveLineNumber(Class<?> beanType, Method method) {
        String cacheKey = beanType.getName() + "#" + Type.getMethodDescriptor(method);
        return LINE_CACHE.computeIfAbsent(cacheKey, key -> readLineNumberFromBytecode(beanType, method));
    }

    /**
     * 从字节码解析方法首行行号。
     *
     * @return 行号；失败返回 {@code -1}
     */
    private static int readLineNumberFromBytecode(Class<?> beanType, Method method) {
        String resource = beanType.getSimpleName() + ".class";
        try (InputStream inputStream = beanType.getResourceAsStream(resource)) {
            if (inputStream == null) {
                return -1;
            }

            String methodName = method.getName();
            String methodDescriptor = Type.getMethodDescriptor(method);
            int[] lineHolder = {-1};

            ClassReader classReader = new ClassReader(inputStream);
            classReader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if (!methodName.equals(name) || !methodDescriptor.equals(descriptor)) {
                        return null;
                    }
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitLineNumber(int line, org.springframework.asm.Label label) {
                            if (lineHolder[0] < 0) {
                                lineHolder[0] = line;
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_FRAMES);

            return lineHolder[0];
        } catch (Exception ignored) {
            return -1;
        }
    }
}
