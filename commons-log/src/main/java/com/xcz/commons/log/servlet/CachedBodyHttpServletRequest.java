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
 * 可重复读取请求体的 Request 包装。
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    /**
     * 缓存的请求体字节
     */
    private final byte[] cachedBody;

    /**
     * 读取并缓存原始请求体。
     *
     * @throws IOException 读取输入流失败
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    /**
     * 返回可重复读取的输入流
     */
    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(this.cachedBody);
    }

    /**
     * 返回基于缓存的 Reader
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(this.cachedBody), StandardCharsets.UTF_8));
    }

    /**
     * 沿 Wrapper 链查找本包装实例。
     *
     * @return 实例；未找到返回 {@code null}
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
     * 获取缓存的请求体字符串。
     *
     * @return UTF-8 字符串；空 body 返回 {@code null}
     */
    public String getBody() {
        if (cachedBody == null || cachedBody.length == 0) {
            return null;
        }
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    /**
     * 基于 byte[] 的可重复读取输入流
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
