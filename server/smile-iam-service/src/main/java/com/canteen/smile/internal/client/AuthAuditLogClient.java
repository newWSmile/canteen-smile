package com.canteen.smile.internal.client;

import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.dto.AuthAuditLogInternalResponse;
import com.canteen.smile.internal.client.dto.AuthAuditLogSearchInternalRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** IAM 查询 Auth 自有认证审计使用的 HMAC RestClient。 */
@Component
@RequiredArgsConstructor
public class AuthAuditLogClient {

    /** 当前 Client 日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(AuthAuditLogClient.class);

    /** Auth 审计内部查询路径。 */
    private static final String SEARCH_PATH = "/internal/auth/v1/audit-logs/search";

    /** 已配置静态地址、超时和 HMAC 的 Auth Client。 */
    private final RestClient authRestClient;

    /** @param request 经过 IAM 权限收敛的查询条件 @return Auth 审计分页 */
    public PageResult<AuthAuditLogInternalResponse> page(AuthAuditLogSearchInternalRequest request) {
        try {
            /** Auth 返回的统一分页响应。 */
            ApiResponse<PageResult<AuthAuditLogInternalResponse>> response = authRestClient.post()
                    .uri(SEARCH_PATH)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (response == null || !"0".equals(response.code()) || response.data() == null) {
                throw unavailable();
            }
            return response.data();
        } catch (RestClientException exception) {
            log.warn("Auth audit search failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    /** @return Auth 审计服务不可用业务异常 */
    private BusinessException unavailable() {
        return new BusinessException("IAM_2902", "认证审计服务暂时不可用，请稍后重试", 502);
    }
}
