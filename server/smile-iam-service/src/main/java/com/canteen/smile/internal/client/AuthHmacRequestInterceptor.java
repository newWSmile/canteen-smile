package com.canteen.smile.internal.client;

import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.infrastructure.security.InternalHmacHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/** 为 IAM → Auth RestClient 请求添加 HMAC v1 签名。 */
@RequiredArgsConstructor
public class AuthHmacRequestInterceptor implements ClientHttpRequestInterceptor {

    /** HMAC 客户端配置。 */
    private final AuthHmacClientProperties properties;

    /** {@inheritDoc} */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new IllegalStateException("INTERNAL_HMAC_AUTH_TO_IAM_SECRET is required");
        }
        /** Unix 秒级时间戳。 */
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        /** 单次随机数。 */
        String nonce = UUID.randomUUID().toString().replace("-", "");
        /** 跨服务事件标识。 */
        String requestedEventId = request.getHeaders().getFirst(InternalHmacHeaders.EVENT_ID);
        String eventId = requestedEventId == null || requestedEventId.isBlank()
                ? UUID.randomUUID().toString()
                : requestedEventId;
        /** 请求体 SHA-256 摘要。 */
        String contentHash = HmacRequestSigner.sha256Hex(body);
        /** HMAC v1 规范请求串。 */
        String canonical = HmacRequestSigner.canonicalRequest(request.getMethod().name(),
                request.getURI().getRawPath(), request.getURI().getRawQuery(), contentHash,
                timestamp, nonce, properties.getCallerId(), eventId);
        /** HMAC-SHA256 签名。 */
        String signature = HmacRequestSigner.sign(properties.getSecret(), canonical);
        request.getHeaders().set(InternalHmacHeaders.CALLER_ID, properties.getCallerId());
        request.getHeaders().set(InternalHmacHeaders.KEY_ID, properties.getKeyId());
        request.getHeaders().set(InternalHmacHeaders.TIMESTAMP, timestamp);
        request.getHeaders().set(InternalHmacHeaders.NONCE, nonce);
        request.getHeaders().set(InternalHmacHeaders.EVENT_ID, eventId);
        request.getHeaders().set(InternalHmacHeaders.CONTENT_SHA_256, contentHash);
        request.getHeaders().set(InternalHmacHeaders.SIGNATURE, signature);
        return execution.execute(request, body);
    }
}
