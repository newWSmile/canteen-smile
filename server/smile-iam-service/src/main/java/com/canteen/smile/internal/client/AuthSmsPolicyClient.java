package com.canteen.smile.internal.client;

import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.dto.SmsRateLimitPolicyUpdateInternalRequest;
import com.canteen.smile.internal.client.dto.SmsRuntimePolicyInternalResponse;
import com.canteen.smile.internal.client.dto.SmsSecurityPolicyUpdateInternalRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** IAM 管理 Auth 自有短信运行策略使用的 HMAC RestClient。 */
@Component
@RequiredArgsConstructor
public class AuthSmsPolicyClient {

    /** 日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(AuthSmsPolicyClient.class);
    /** 当前策略路径。 */
    private static final String POLICY_PATH = "/internal/auth/v1/sms-policy";
    /** 限流策略修改路径。 */
    private static final String RATE_LIMIT_PATH = POLICY_PATH + "/rate-limits";
    /** 安全策略修改路径。 */
    private static final String SECURITY_PATH = POLICY_PATH + "/security";

    /** 带 HMAC、静态地址和超时的 Auth Client。 */
    private final RestClient authRestClient;

    /** @return 当前全局短信运行策略 */
    public SmsRuntimePolicyInternalResponse current() {
        return exchange(() -> authRestClient.get().uri(POLICY_PATH).retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<SmsRuntimePolicyInternalResponse>>() { }));
    }

    /** @param request 限流策略更新契约 @return 更新后的策略 */
    public SmsRuntimePolicyInternalResponse updateRateLimits(SmsRateLimitPolicyUpdateInternalRequest request) {
        return exchange(() -> authRestClient.put().uri(RATE_LIMIT_PATH).body(request).retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<SmsRuntimePolicyInternalResponse>>() { }));
    }

    /** @param request 安全策略更新契约 @return 更新后的策略 */
    public SmsRuntimePolicyInternalResponse updateSecurity(SmsSecurityPolicyUpdateInternalRequest request) {
        return exchange(() -> authRestClient.put().uri(SECURITY_PATH).body(request).retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<SmsRuntimePolicyInternalResponse>>() { }));
    }

    /** 执行内部请求并保留再认证或并发冲突语义。 */
    private SmsRuntimePolicyInternalResponse exchange(PolicyExchange exchange) {
        try {
            ApiResponse<SmsRuntimePolicyInternalResponse> response = exchange.execute();
            if (response == null || !"0".equals(response.code()) || response.data() == null) throw unavailable();
            return response.data();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401) {
                throw new BusinessException("IAM_2961", "管理员再认证已失效，请重新验证当前密码", 401);
            }
            if (exception.getStatusCode().value() == 409) {
                throw new BusinessException("IAM_2962", "短信设置已被其他管理员修改，请刷新后重试", 409);
            }
            log.warn("Auth SMS policy request rejected: status={}", exception.getStatusCode().value());
            throw unavailable();
        } catch (RestClientException exception) {
            log.warn("Auth SMS policy request failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    /** @return Auth 短信策略服务不可用异常 */
    private BusinessException unavailable() {
        return new BusinessException("IAM_2960", "短信设置服务暂时不可用，请稍后重试", 502);
    }

    /** 延迟执行具体 RestClient 请求。 */
    @FunctionalInterface
    private interface PolicyExchange {
        /** @return Auth 统一响应 */
        ApiResponse<SmsRuntimePolicyInternalResponse> execute();
    }
}
