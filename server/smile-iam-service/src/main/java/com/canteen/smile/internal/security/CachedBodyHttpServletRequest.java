package com.canteen.smile.internal.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 可重复读取请求体的内部 HMAC 请求包装器。 */
final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    /** 请求体原始字节。 */
    private final byte[] body;

    /**
     * 读取并缓存请求体。
     *
     * @param request 原始 Servlet 请求
     * @throws IOException 请求体读取失败
     */
    CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.body = request.getInputStream().readAllBytes();
    }

    /** @return 请求体原始字节的防御性副本 */
    byte[] body() {
        return body.clone();
    }

    /** @return 每次从头读取缓存请求体的 Servlet 输入流 */
    @Override
    public ServletInputStream getInputStream() {
        /** 当前读取使用的字节数组流。 */
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            /** @return 下一个请求体字节 */
            @Override
            public int read() {
                return inputStream.read();
            }

            /** @return 缓存请求体是否已经读完 */
            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            /** @return 缓存请求体读取始终可以立即进行 */
            @Override
            public boolean isReady() {
                return true;
            }

            /**
             * 当前包装器只支持同步读取。
             *
             * @param readListener 异步读取监听器
             */
            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("Async request body reading is not supported");
            }
        };
    }

    /** @return 使用 UTF-8 读取缓存请求体的字符流 */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
