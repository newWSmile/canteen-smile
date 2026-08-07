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

/** 在 DTO 解析前校验 IAM `/internal/iam/**` 请求的 HMAC 和 Nonce。 */
@Component
@RequiredArgsConstructor
public class IamInternalHmacFilter extends OncePerRequestFilter {

    /** 内部签名无效错误码。 */
    private static final String INVALID_SIGNATURE_CODE = "INTERNAL_3001";

    /** 内部 Nonce 重放错误码。 */
    private static final String REPLAY_CODE = "INTERNAL_3002";

    /** IAM 内部接口路径前缀。 */
    private static final String INTERNAL_PATH_PREFIX = "/internal/iam/";

    /** HMAC 校验配置。 */
    private final IamInternalHmacProperties properties;

    /** Redis 字符串操作模板。 */
    private final StringRedisTemplate redisTemplate;

    /** 统一 Redis Key 构建器。 */
    private final RedisKeyBuilder redisKeyBuilder;

    /** Jackson 响应序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 校验内部请求并在校验成功后传递可重复读取的请求体。
     *
     * @param request 原始请求
     * @param response 原始响应
     * @param filterChain Servlet 过滤器链
     * @throws ServletException 过滤器链异常
     * @throws IOException 请求读写异常
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        /** 缓存请求体后的请求。 */
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        /** 调用方服务标识。 */
        String callerId = request.getHeader(InternalHmacHeaders.CALLER_ID);
        /** 密钥版本标识。 */
        String keyId = request.getHeader(InternalHmacHeaders.KEY_ID);
        /** Unix 秒级时间戳文本。 */
        String timestamp = request.getHeader(InternalHmacHeaders.TIMESTAMP);
        /** 防重放随机数。 */
        String nonce = request.getHeader(InternalHmacHeaders.NONCE);
        /** 请求事件 ID。 */
        String eventId = request.getHeader(InternalHmacHeaders.EVENT_ID);
        /** 客户端请求体摘要。 */
        String providedContentHash = request.getHeader(InternalHmacHeaders.CONTENT_SHA_256);
        /** 客户端 HMAC 签名。 */
        String providedSignature = request.getHeader(InternalHmacHeaders.SIGNATURE);

        if (!hasRequiredConfigurationAndHeaders(callerId, keyId, timestamp, nonce, eventId)) {
            writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, INVALID_SIGNATURE_CODE, "内部请求签名无效");
            return;
        }

        /** 请求时间戳数值。 */
        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, INVALID_SIGNATURE_CODE, "内部请求签名无效");
            return;
        }
        if (Math.abs(Instant.now().getEpochSecond() - timestampSeconds) > properties.getAllowedClockSkewSeconds()) {
            writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, INVALID_SIGNATURE_CODE, "内部请求签名已过期");
            return;
        }

        /** 服务端计算的请求体摘要。 */
        String actualContentHash = HmacRequestSigner.sha256Hex(cachedRequest.body());
        if (!HmacRequestSigner.constantTimeEquals(actualContentHash, providedContentHash)) {
            writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, INVALID_SIGNATURE_CODE, "内部请求签名无效");
            return;
        }

        /** 当前请求的 HMAC v1 规范串。 */
        String canonicalRequest = HmacRequestSigner.canonicalRequest(
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                actualContentHash,
                timestamp,
                nonce,
                callerId,
                eventId
        );
        /** 服务端计算的期望签名。 */
        String expectedSignature = HmacRequestSigner.sign(properties.getSecret(), canonicalRequest);
        if (!HmacRequestSigner.constantTimeEquals(expectedSignature, providedSignature)) {
            writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, INVALID_SIGNATURE_CODE, "内部请求签名无效");
            return;
        }

        /** 防重放 Redis Key。 */
        String nonceKey = redisKeyBuilder.build("internal", "hmac", "nonce", callerId, nonce);
        /** Nonce 首次占用结果。 */
        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(
                nonceKey,
                eventId,
                Duration.ofSeconds(properties.getNonceTtlSeconds())
        );
        if (!Boolean.TRUE.equals(reserved)) {
            writeFailure(response, HttpServletResponse.SC_CONFLICT, REPLAY_CODE, "内部请求已处理");
            return;
        }
        filterChain.doFilter(cachedRequest, response);
    }

    /**
     * 仅拦截 IAM 内部接口。
     *
     * @param request 当前请求
     * @return 是否跳过当前过滤器
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    /**
     * 校验签名配置和必需请求头。
     *
     * @param callerId 调用方标识
     * @param keyId 密钥版本
     * @param timestamp 时间戳
     * @param nonce 防重放随机数
     * @param eventId 请求事件 ID
     * @return 是否具备有效配置和非空请求头
     */
    private boolean hasRequiredConfigurationAndHeaders(
            String callerId,
            String keyId,
            String timestamp,
            String nonce,
            String eventId
    ) {
        return properties.getSecret() != null
                && !properties.getSecret().isBlank()
                && properties.getAllowedCallerId().equals(callerId)
                && properties.getKeyId().equals(keyId)
                && timestamp != null
                && !timestamp.isBlank()
                && nonce != null
                && !nonce.isBlank()
                && eventId != null
                && !eventId.isBlank();
    }

    /**
     * 直接写出过滤器阶段的统一失败响应。
     *
     * @param response Servlet 响应
     * @param status HTTP 状态
     * @param code 稳定错误码
     * @param message 对外提示
     * @throws IOException 响应写入失败
     */
    private void writeFailure(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(code, message));
    }
}
