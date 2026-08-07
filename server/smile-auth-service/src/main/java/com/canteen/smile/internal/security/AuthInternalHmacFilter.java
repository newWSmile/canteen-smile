package com.canteen.smile.internal.security;

import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.infrastructure.redis.RedisKeyBuilder;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.infrastructure.security.InternalHmacHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/** 在 DTO 解析前校验 Auth 内部接口的 HMAC 签名和 Nonce。 */
@Component
@RequiredArgsConstructor
public class AuthInternalHmacFilter extends OncePerRequestFilter {

    /** 内部接口路径前缀。 */
    private static final String INTERNAL_PATH_PREFIX = "/internal/auth/";

    /** 内部签名无效错误码。 */
    private static final String INVALID_SIGNATURE_CODE = "INTERNAL_3001";

    /** Nonce 重放错误码。 */
    private static final String REPLAY_CODE = "INTERNAL_3002";

    /** HMAC 校验配置。 */
    private final AuthInternalHmacProperties properties;

    /** Redis 字符串操作模板。 */
    private final StringRedisTemplate redisTemplate;

    /** 统一 Redis Key 构建器。 */
    private final RedisKeyBuilder redisKeyBuilder;

    /** Jackson 响应序列化器。 */
    private final ObjectMapper objectMapper;

    /** {@inheritDoc} */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        /** 缓存请求体后的请求。 */
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        /** 调用方标识。 */
        String callerId = request.getHeader(InternalHmacHeaders.CALLER_ID);
        /** 密钥版本。 */
        String keyId = request.getHeader(InternalHmacHeaders.KEY_ID);
        /** Unix 秒时间戳。 */
        String timestamp = request.getHeader(InternalHmacHeaders.TIMESTAMP);
        /** 单次随机数。 */
        String nonce = request.getHeader(InternalHmacHeaders.NONCE);
        /** 跨服务事件标识。 */
        String eventId = request.getHeader(InternalHmacHeaders.EVENT_ID);
        /** 客户端请求体摘要。 */
        String contentHash = request.getHeader(InternalHmacHeaders.CONTENT_SHA_256);
        /** 客户端签名。 */
        String signature = request.getHeader(InternalHmacHeaders.SIGNATURE);
        if (!validHeaders(callerId, keyId, timestamp, nonce, eventId)) {
            failure(response, 401, INVALID_SIGNATURE_CODE, "内部请求签名无效");
            return;
        }
        /** 数值化时间戳。 */
        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            failure(response, 401, INVALID_SIGNATURE_CODE, "内部请求签名无效");
            return;
        }
        if (Math.abs(Instant.now().getEpochSecond() - timestampSeconds) > properties.getAllowedClockSkewSeconds()) {
            failure(response, 401, INVALID_SIGNATURE_CODE, "内部请求签名已过期");
            return;
        }
        /** 服务端计算的请求体摘要。 */
        String actualHash = HmacRequestSigner.sha256Hex(cachedRequest.body());
        /** 服务端计算的规范请求串。 */
        String canonical = HmacRequestSigner.canonicalRequest(request.getMethod(), request.getRequestURI(),
                request.getQueryString(), actualHash, timestamp, nonce, callerId, eventId);
        /** 服务端期望签名。 */
        String expected = HmacRequestSigner.sign(properties.getSecret(), canonical);
        if (!HmacRequestSigner.constantTimeEquals(actualHash, contentHash)
                || !HmacRequestSigner.constantTimeEquals(expected, signature)) {
            failure(response, 401, INVALID_SIGNATURE_CODE, "内部请求签名无效");
            return;
        }
        /** 防重放 Redis Key。 */
        String nonceKey = redisKeyBuilder.build("internal", "hmac", "nonce", callerId, nonce);
        /** Nonce 首次占用结果。 */
        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(
                nonceKey, eventId, Duration.ofSeconds(properties.getNonceTtlSeconds()));
        if (!Boolean.TRUE.equals(reserved)) {
            failure(response, 409, REPLAY_CODE, "内部请求已处理");
            return;
        }
        chain.doFilter(cachedRequest, response);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    /** @return 是否具备完整且匹配的签名配置和请求头 */
    private boolean validHeaders(String callerId, String keyId, String timestamp, String nonce, String eventId) {
        return properties.getSecret() != null && !properties.getSecret().isBlank()
                && properties.getAllowedCallerId().equals(callerId) && properties.getKeyId().equals(keyId)
                && timestamp != null && !timestamp.isBlank()
                && nonce != null && !nonce.isBlank()
                && eventId != null && !eventId.isBlank();
    }

    /** 写出统一失败响应。 */
    private void failure(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(code, message));
    }
}
