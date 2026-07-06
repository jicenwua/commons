package com.xcz.member.log.support;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析 Spring MVC {@link HandlerMethod}，生成 IDE 可点击跳转的短格式日志链接。
 * <p>
 * 使用 {@code 类名.方法名(类名.java:行号)} 格式，无需全限定类名，例如：
 * {@code MobiShopController.getUserList(MobiShopController.java:45)}
 */
public final class HandlerMethodLinkResolver {

    /** 方法首行行号缓存，key = 全限定类名#方法描述符 */
    private static final ConcurrentHashMap<String, Integer> LINE_CACHE = new ConcurrentHashMap<>();

    private HandlerMethodLinkResolver() {
    }

    /**
     * Controller 方法跳转信息。
     *
     * @param navigationLink 短格式 IDE 可点击链接；
     *                       非 Controller 请求时为 {@code handler.toString()}
     */
    public record ControllerMethodLink(String navigationLink) {
    }

    /**
     * 从拦截器 handler 解析短格式跳转链接（不含包名）。
     *
     * @param handler {@link HandlerInterceptor} 传入的 handler
     * @return 解析结果；非 {@link HandlerMethod} 时 {@code navigationLink} 为 {@code handler.toString()}
     */
    public static ControllerMethodLink resolve(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return new ControllerMethodLink(String.valueOf(handler));
        }
        //获取方法的名称
        Class<?> beanType = handlerMethod.getBeanType();
        Method method = handlerMethod.getMethod();
        String simpleClassName = beanType.getSimpleName();
        String methodName = method.getName();

        int line = resolveLineNumber(beanType, method);
        // 短文件名 + 行号，IDE 在模块内可识别并跳转
        String fileRef = line > 0 ? simpleClassName + ".java:" + line : simpleClassName + ".java";
        String navigationLink = simpleClassName + "." + methodName + "(" + fileRef + ")";

        return new ControllerMethodLink(navigationLink);
    }

    /**
     * 通过 ASM 读取字节码 LineNumberTable，获取方法首行源码行号（带缓存）。
     *
     * @param beanType Controller 类
     * @param method   目标方法
     * @return 源码行号；解析失败时返回 {@code -1}
     */
    private static int resolveLineNumber(Class<?> beanType, Method method) {
        String cacheKey = beanType.getName() + "#" + Type.getMethodDescriptor(method);
        return LINE_CACHE.computeIfAbsent(cacheKey, key -> readLineNumberFromBytecode(beanType, method));
    }

    /**
     * 从 .class 字节码解析方法对应的首个行号。
     *
     * @param beanType Controller 类
     * @param method   目标方法
     * @return 源码行号；读取失败时返回 {@code -1}
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
                            // 只取方法内第一个行号，作为跳转锚点
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
