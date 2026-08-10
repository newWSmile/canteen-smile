package com.canteen.smile.internal.client;

import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.InternalHmacHeaders;
import com.canteen.smile.internal.client.dto.TenantAccountProvisionInternalRequest;
import com.canteen.smile.internal.client.dto.TenantAccountProvisionInternalResponse;
import com.canteen.smile.internal.client.dto.TenantActivationTicketInternalResponse;
import com.canteen.smile.internal.client.dto.TenantPasswordResetTicketInternalRequest;
import com.canteen.smile.internal.client.dto.TenantPasswordResetTicketInternalResponse;
import com.canteen.smile.internal.client.dto.ConsumeReauthTicketInternalRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** IAM 初始化租户账号时使用的 Auth v1 Client。 */
@Component
@RequiredArgsConstructor
public class AuthTenantAccountClient {

    /** 当前 Client 日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(AuthTenantAccountClient.class);

    /** Auth 暂时不可用错误码。 */
    private static final String AUTH_UNAVAILABLE_CODE = "IAM_2205";

    /** 内部接口路径。 */
    private static final String PROVISION_PATH = "/internal/auth/v1/tenant-accounts/{accountId}/provision";

    /** 激活票据签发内部路径。 */
    private static final String ACTIVATION_TICKETS_PATH =
            "/internal/auth/v1/tenant-accounts/{accountId}/activation-tickets";

    /** 密码恢复票据签发内部路径。 */
    private static final String PASSWORD_RESET_TICKETS_PATH =
            "/internal/auth/v1/tenant-accounts/{accountId}/password-reset-tickets";

    /** 再认证票据原子消费内部路径。 */
    private static final String REAUTH_TICKET_CONSUME_PATH =
            "/internal/auth/v1/reauth-tickets/actions/consume";

    /** 已配置 HMAC 与超时的 Auth RestClient。 */
    private final RestClient authRestClient;

    /**
     * 幂等创建待激活租户账号凭证容器。
     *
     * @param accountId 租户账号 ID
     * @param tenantId 租户 ID
     * @param organizationId 所属机构 ID
     */
    public void provision(long accountId, long tenantId, long organizationId) {
        provision(null, accountId, tenantId, organizationId);
    }

    /** 使用指定 Outbox 事件 ID 幂等创建租户账号凭证容器。 */
    public void provision(String eventId, long accountId, long tenantId, long organizationId) {
        try {
            /** Auth 统一响应。 */
            RestClient.RequestBodySpec requestSpec = authRestClient.post().uri(PROVISION_PATH, accountId);
            if (eventId != null && !eventId.isBlank()) {
                requestSpec.header(InternalHmacHeaders.EVENT_ID, eventId);
            }
            ApiResponse<TenantAccountProvisionInternalResponse> response = requestSpec
                    .body(new TenantAccountProvisionInternalRequest(tenantId + "", organizationId + ""))
                    .retrieve().body(new ParameterizedTypeReference<>() { });
            if (response == null || !"0".equals(response.code()) || response.data() == null
                    || !Long.toString(accountId).equals(response.data().accountId())) {
                throw unavailable();
            }
        } catch (RestClientException exception) {
            log.warn("Auth tenant credential provision failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    /** @param accountId 租户账号 ID @return 只展示一次的激活票据 */
    public TenantActivationTicketInternalResponse issueActivationTicket(long accountId) {
        try {
            ApiResponse<TenantActivationTicketInternalResponse> response = authRestClient.post()
                    .uri(ACTIVATION_TICKETS_PATH, accountId)
                    .retrieve().body(new ParameterizedTypeReference<>() { });
            if (response == null || !"0".equals(response.code()) || response.data() == null
                    || response.data().activationTicket() == null) {
                throw unavailable();
            }
            return response.data();
        } catch (RestClientException exception) {
            log.warn("Auth activation ticket request failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    /**
     * 消费平台再认证票据并生成租户所有者一次性密码恢复票据。
     *
     * @param accountId 租户所有者账号 ID
     * @param initiatorId 发起平台身份 ID
     * @param reauthTicket 五分钟一次性再认证票据
     * @return 只展示一次的密码恢复票据
     */
    public TenantPasswordResetTicketInternalResponse issuePasswordResetTicket(
            long accountId,
            long initiatorId,
            String reauthTicket
    ) {
        try {
            TenantPasswordResetTicketInternalRequest request =
                    new TenantPasswordResetTicketInternalRequest(
                            "PLATFORM_IDENTITY",
                            Long.toString(initiatorId),
                            reauthTicket,
                            "TENANT_OWNER_PASSWORD_RESET"
                    );
            ApiResponse<TenantPasswordResetTicketInternalResponse> response = authRestClient.post()
                    .uri(PASSWORD_RESET_TICKETS_PATH, accountId)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (response == null || !"0".equals(response.code()) || response.data() == null
                    || response.data().resetTicket() == null) {
                throw unavailable();
            }
            return response.data();
        } catch (RestClientResponseException exception) {
            log.warn(
                    "Auth password reset ticket rejected: status={}",
                    exception.getStatusCode().value()
            );
            if (exception.getStatusCode().value() == 401) {
                throw new BusinessException("IAM_2306", "再认证已失效，请重新验证当前密码", 401);
            }
            throw unavailable();
        } catch (RestClientException exception) {
            log.warn("Auth password reset ticket request failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    /**
     * 原子消费当前租户管理员绑定单一动作的再认证票据。
     *
     * @param accountId 当前租户管理员账号 ID
     * @param reauthTicket 五分钟一次性票据
     * @param allowedAction 票据绑定动作
     */
    public void consumeTenantReauthTicket(long accountId, String reauthTicket, String allowedAction) {
        try {
            ApiResponse<Void> response = authRestClient.post()
                    .uri(REAUTH_TICKET_CONSUME_PATH)
                    .body(new ConsumeReauthTicketInternalRequest(
                            reauthTicket, "TENANT_ACCOUNT", Long.toString(accountId), allowedAction
                    ))
                    .retrieve().body(new ParameterizedTypeReference<>() { });
            if (response == null || !"0".equals(response.code())) throw unavailable();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401) {
                throw new BusinessException("IAM_2806", "管理员再认证已失效，请重新验证当前密码", 401);
            }
            throw authUnavailable();
        } catch (RestClientException exception) {
            log.warn("Auth tenant reauth consume failed: {}", exception.getClass().getSimpleName());
            throw authUnavailable();
        }
    }

    /** @return 不带错误编排语义的 Auth 暂时不可用异常 */
    private BusinessException authUnavailable() {
        return new BusinessException(AUTH_UNAVAILABLE_CODE, "认证服务暂时不可用，请稍后重试", 502);
    }

    /** @return 统一 Auth 暂时不可用异常 */
    private BusinessException unavailable() {
        return new BusinessException(AUTH_UNAVAILABLE_CODE, "认证服务暂时不可用，租户已保留并等待重试", 502);
    }
}
