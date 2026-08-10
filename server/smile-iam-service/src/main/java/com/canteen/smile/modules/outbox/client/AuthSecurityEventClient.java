package com.canteen.smile.modules.outbox.client;

import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.infrastructure.security.InternalHmacHeaders;
import com.canteen.smile.internal.client.dto.SecurityEventInternalRequest;
import com.canteen.smile.internal.client.dto.SecurityEventInternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** IAM Outbox 向 Auth 投递账号安全事件的显式 REST Client。 */
@Component
@RequiredArgsConstructor
public class AuthSecurityEventClient {

    /** Auth v1 内部安全事件路径。 */
    private static final String SECURITY_EVENT_PATH = "/internal/auth/v1/security-events";

    /** 已配置静态地址、超时与 HMAC 的 Auth RestClient。 */
    private final RestClient authRestClient;

    /** 幂等投递一条安全事件。 */
    public void deliver(SecurityEventInternalRequest request) {
        try {
            ApiResponse<SecurityEventInternalResponse> response = authRestClient.post()
                    .uri(SECURITY_EVENT_PATH)
                    .header(InternalHmacHeaders.EVENT_ID, request.eventId())
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (response == null || !"0".equals(response.code()) || response.data() == null
                    || !request.eventId().equals(response.data().eventId())) {
                throw new OutboxDeliveryException("AUTH_INVALID_EVENT_RESPONSE", false);
            }
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            boolean permanent = status == 400 || status == 404 || status == 422;
            throw new OutboxDeliveryException("AUTH_HTTP_" + status, permanent, exception);
        } catch (RestClientException exception) {
            throw new OutboxDeliveryException("AUTH_UNAVAILABLE", false, exception);
        }
    }
}
