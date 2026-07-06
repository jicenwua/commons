package com.xcz.commons.log.servlet;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取请求体的 {@link HttpServletRequest} 包装类。
 * <p>
 * Servlet 的 {@link ServletInputStream} 默认只能读一次；
 * 通过缓存 byte[]，日志拦截器与 Controller 均可读取 body。
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    /** 缓存的请求体字节数组 */
    private final byte[] cachedBody;

    /**
     * 读取并缓存原始请求体。
     *
     * @param request 原始请求（由 {@link LogFilter} 传入，已排除 multipart）
     * @throws IOException 读取输入流失败时抛出
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // 一次性读入内存，后续通过包装流反复供给 Spring MVC / 日志模块
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    /**
     * 返回基于缓存的可重复读取输入流。
     */
    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(this.cachedBody);
    }

    /**
     * 返回基于缓存的字符流 reader。
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(this.cachedBody), StandardCharsets.UTF_8));
    }

    /**
     * 沿 {@link HttpServletRequestWrapper} 链查找缓存包装实例。
     * <p>
     * 拦截器拿到的可能是外层 Wrapper，不能直接 {@code instanceof}。
     *
     * @param request 当前请求（可能为多层 Wrapper）
     * @return 缓存包装实例；未找到时返回 {@code null}
     */
    public static CachedBodyHttpServletRequest resolve(HttpServletRequest request) {
        HttpServletRequest current = request;
        while (current != null) {
            if (current instanceof CachedBodyHttpServletRequest cached) {
                return cached;
            }
            if (current instanceof HttpServletRequestWrapper wrapper) {
                current = (HttpServletRequest) wrapper.getRequest();
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * 获取缓存的请求体字符串，供日志打印使用。
     *
     * @return UTF-8 字符串；body 为空时返回 {@code null}
     */
    public String getBody() {
        if (cachedBody == null || cachedBody.length == 0) {
            return null;
        }
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    /**
     * 基于 byte[] 的 {@link ServletInputStream} 实现，支持多次读取。
     */
    private static class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        CachedServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("异步读取不支持");
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
