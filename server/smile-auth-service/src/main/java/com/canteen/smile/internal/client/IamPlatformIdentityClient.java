package com.canteen.smile.internal.client;

import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.dto.BootstrapPlatformIdentityInternalRequest;
import com.canteen.smile.internal.client.dto.PlatformIdentityInternalResponse;
import com.canteen.smile.internal.client.dto.UsernameLoginResolutionInternalRequest;
import com.canteen.smile.internal.client.dto.UsernameLoginResolutionInternalResponse;
import com.canteen.smile.internal.client.dto.TenantAccountActivationContextInternalResponse;
import com.canteen.smile.internal.client.dto.MobileAccountLoginCandidateInternalResponse;
import com.canteen.smile.internal.client.dto.MobileAccountLoginResolutionInternalRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.List;

/** Auth 编排平台身份时使用的 IAM v1 Client。 */
@Component
@RequiredArgsConstructor
public class IamPlatformIdentityClient {

    /** 当前 Client 日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(IamPlatformIdentityClient.class);

    /** IAM 内部调用失败错误码。 */
    private static final String IAM_UNAVAILABLE_CODE = "AUTH_1011";

    /** Auth 对外暴露的首次引导已关闭错误码。 */
    private static final String AUTH_BOOTSTRAP_CLOSED_CODE = "AUTH_1010";

    /** IAM 返回的首次引导已关闭错误码。 */
    private static final String IAM_BOOTSTRAP_CLOSED_CODE = "IAM_2013";

    /** 已配置 HMAC 和超时的 IAM RestClient。 */
    private final RestClient iamRestClient;

    /** 用于解析 IAM 统一失败响应的 Jackson 对象映射器。 */
    private final ObjectMapper objectMapper;

    /**
     * 幂等创建首位平台身份。
     *
     * @param request 平台身份资料
     * @return IAM 平台身份内部快照
     */
    public PlatformIdentityInternalResponse bootstrap(BootstrapPlatformIdentityInternalRequest request) {
        try {
            /** IAM 统一响应。 */
            ApiResponse<PlatformIdentityInternalResponse> response = iamRestClient.post()
                    .uri(IamInternalApiPaths.PLATFORM_IDENTITY_BOOTSTRAP)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return requireSuccess(response);
        } catch (RestClientResponseException exception) {
            throw translateBootstrapFailure(exception);
        } catch (RestClientException exception) {
            log.warn("IAM bootstrap transport request failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException(IAM_UNAVAILABLE_CODE, "平台身份服务暂时不可用", 502);
        }
    }

    /**
     * Auth 凭证就绪后激活平台身份。
     *
     * @param identityId 平台身份 ID
     * @return 激活后的平台身份内部快照
     */
    public PlatformIdentityInternalResponse activate(long identityId) {
        try {
            /** IAM 统一响应。 */
            ApiResponse<PlatformIdentityInternalResponse> response = iamRestClient.post()
                    .uri(IamInternalApiPaths.PLATFORM_IDENTITY_ACTIVATE, identityId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return requireSuccess(response);
        } catch (RestClientException exception) {
            throw new BusinessException(IAM_UNAVAILABLE_CODE, "平台身份服务暂时不可用", 502);
        }
    }

    /**
     * 通过 IAM 解析用户名对应的可登录主体，Auth 不复制 IAM 用户资料。
     *
     * @param request 应用入口和用户名
     * @return 登录解析结果
     */
    public UsernameLoginResolutionInternalResponse resolveUsername(
            UsernameLoginResolutionInternalRequest request
    ) {
        try {
            /** IAM 统一响应。 */
            ApiResponse<UsernameLoginResolutionInternalResponse> response = iamRestClient.post()
                    .uri(IamInternalApiPaths.USERNAME_LOGIN_RESOLUTION)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || !"0".equals(response.code()) || response.data() == null) {
                throw new BusinessException(IAM_UNAVAILABLE_CODE, "平台身份服务返回无效响应", 502);
            }
            return response.data();
        } catch (RestClientException exception) {
            throw new BusinessException(IAM_UNAVAILABLE_CODE, "平台身份服务暂时不可用", 502);
        }
    }

    /**
     * 批量解析手机号登录候选账号，IAM 会再次校验账号、租户、机构和有效期状态。
     *
     * @param request 应用入口与候选账号 ID
     * @return 当前可登录候选账号
     */
    public List<MobileAccountLoginCandidateInternalResponse> resolveMobileAccounts(
            MobileAccountLoginResolutionInternalRequest request
    ) {
        try {
            ApiResponse<List<MobileAccountLoginCandidateInternalResponse>> response = iamRestClient.post()
                    .uri(IamInternalApiPaths.MOBILE_ACCOUNT_LOGIN_RESOLUTION)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (response == null || !"0".equals(response.code()) || response.data() == null) {
                throw new BusinessException(IAM_UNAVAILABLE_CODE, "租户身份服务返回无效响应", 502);
            }
            return response.data();
        } catch (RestClientException exception) {
            throw new BusinessException(IAM_UNAVAILABLE_CODE, "租户身份服务暂时不可用", 502);
        }
    }

    /** @param accountId 租户账号 ID @return 可展示的激活上下文 */
    public TenantAccountActivationContextInternalResponse activationContext(long accountId) {
        return tenantActivationRequest(IamInternalApiPaths.TENANT_ACCOUNT_ACTIVATION_CONTEXT, accountId, false);
    }

    /** @param accountId 租户账号 ID @return IAM 激活后的账号上下文 */
    public TenantAccountActivationContextInternalResponse activateTenantAccount(long accountId) {
        return tenantActivationRequest(IamInternalApiPaths.TENANT_ACCOUNT_ACTIVATE, accountId, true);
    }

    /** @param accountId 租户账号 ID @return IAM 完成密码恢复后的账号上下文 */
    public TenantAccountActivationContextInternalResponse completeTenantAccountPasswordReset(long accountId) {
        return tenantActivationRequest(
                IamInternalApiPaths.TENANT_ACCOUNT_COMPLETE_PASSWORD_RESET,
                accountId,
                true
        );
    }

    /**
     * 执行租户账号激活相关的 HMAC 内部请求。
     *
     * @param path 路径模板
     * @param accountId 账号 ID
     * @param post 是否使用 POST
     * @return IAM 激活上下文
     */
    private TenantAccountActivationContextInternalResponse tenantActivationRequest(
            String path,
            long accountId,
            boolean post
    ) {
        try {
            ApiResponse<TenantAccountActivationContextInternalResponse> response = post
                    ? iamRestClient.post().uri(path, accountId).retrieve()
                            .body(new ParameterizedTypeReference<>() { })
                    : iamRestClient.get().uri(path, accountId).retrieve()
                            .body(new ParameterizedTypeReference<>() { });
            if (response == null || !"0".equals(response.code()) || response.data() == null) {
                throw new BusinessException(IAM_UNAVAILABLE_CODE, "租户身份服务返回无效响应", 502);
            }
            return response.data();
        } catch (RestClientException exception) {
            throw new BusinessException(IAM_UNAVAILABLE_CODE, "租户身份服务暂时不可用", 502);
        }
    }

    /**
     * 校验 IAM 统一响应并提取业务数据。
     *
     * @param response IAM 统一响应
     * @return 非空平台身份数据
     */
    private PlatformIdentityInternalResponse requireSuccess(
            ApiResponse<PlatformIdentityInternalResponse> response
    ) {
        if (response == null || !"0".equals(response.code()) || response.data() == null) {
            throw new BusinessException(IAM_UNAVAILABLE_CODE, "平台身份服务返回无效响应", 502);
        }
        return response.data();
    }

    /**
     * 将 IAM 首次引导失败响应映射为 Auth 稳定错误码。
     *
     * @param exception IAM 非 2xx 响应异常
     * @return Auth 业务异常
     */
    private BusinessException translateBootstrapFailure(RestClientResponseException exception) {
        /** IAM 统一失败响应，响应体异常时为空。 */
        ApiResponse<Void> failure = readFailureResponse(exception);
        /** IAM 下游错误码，仅用于安全日志和显式契约映射。 */
        String downstreamCode = failure == null ? "UNKNOWN" : failure.code();
        log.warn(
                "IAM bootstrap request rejected: status={}, downstreamCode={}",
                exception.getStatusCode().value(),
                downstreamCode
        );
        if (IAM_BOOTSTRAP_CLOSED_CODE.equals(downstreamCode)) {
            return new BusinessException(
                    AUTH_BOOTSTRAP_CLOSED_CODE,
                    "平台已经完成初始化，不能重复创建平台超级管理员",
                    409
            );
        }
        return new BusinessException(IAM_UNAVAILABLE_CODE, "平台身份服务暂时不可用", 502);
    }

    /**
     * 使用 Jackson 解析 IAM 的统一失败响应，禁止通过字符串猜测错误内容。
     *
     * @param exception IAM 非 2xx 响应异常
     * @return 可识别的统一失败响应；无法解析时返回空
     */
    private ApiResponse<Void> readFailureResponse(RestClientResponseException exception) {
        try {
            return objectMapper.readValue(exception.getResponseBodyAsByteArray(), new TypeReference<>() {
            });
        } catch (IOException parsingException) {
            log.warn(
                    "IAM bootstrap failure response could not be parsed: status={}",
                    exception.getStatusCode().value()
            );
            return null;
        }
    }
}
