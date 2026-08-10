package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.IamPlatformIdentityClient;
import com.canteen.smile.internal.client.dto.UsernameLoginResolutionInternalRequest;
import com.canteen.smile.internal.client.dto.UsernameLoginResolutionInternalResponse;
import com.canteen.smile.modules.auth.dto.PasswordLoginRequest;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.vo.LoginResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 租户管理端用户名密码登录服务。 */
@Service
@RequiredArgsConstructor
public class TenantPasswordLoginService {

    /** 用户名或密码错误码。 */
    private static final String INVALID_CREDENTIALS_CODE = "AUTH_1001";

    /** IAM 登录解析 Client。 */
    private final IamPlatformIdentityClient iamClient;

    /** 凭证数据访问接口。 */
    private final CredentialMapper credentialMapper;

    /** 恒定时间密码验证服务。 */
    private final PasswordVerificationService passwordVerificationService;

    /** 登录失败保护服务。 */
    private final LoginProtectionService loginProtectionService;

    /** 租户设备会话服务。 */
    private final TenantSessionService tenantSessionService;

    /** @param request 登录请求 @param password 解密密码 @param loginIp 登录 IP @return 登录结果 */
    public LoginResultVO login(PasswordLoginRequest request, String password, String loginIp) {
        if (!AuthConstants.TENANT_ADMIN_APP.equals(request.getAppCode())) {
            throw new BusinessException("AUTH_1008", "当前应用入口不匹配", 403);
        }
        loginProtectionService.requirePasswordAttemptAllowed(
                request.getAppCode(), request.getUsername(), request.getCaptchaTicket()
        );
        UsernameLoginResolutionInternalResponse resolution = iamClient.resolveUsername(
                new UsernameLoginResolutionInternalRequest(request.getAppCode(), request.getUsername())
        );
        Long accountId = tenantAccountId(resolution);
        CredentialEntity credential = accountId == null ? null
                : credentialMapper.selectBySubject(AuthConstants.TENANT_ACCOUNT_SUBJECT, accountId);
        if (!passwordVerificationService.matches(password, credential)) {
            loginProtectionService.recordPasswordFailure(
                    request.getAppCode(), request.getUsername(), loginIp, request.getDevice()
            );
            throw new BusinessException(INVALID_CREDENTIALS_CODE, "用户名或密码错误");
        }
        loginProtectionService.resetAfterSuccess(request.getAppCode(), request.getUsername());
        boolean rememberMe = request.isRememberMe() && Boolean.TRUE.equals(resolution.rememberMeEnabled());
        int idleSeconds = rememberMe ? resolution.rememberIdleSeconds() : resolution.idleSeconds();
        int absoluteSeconds = rememberMe ? resolution.rememberAbsoluteSeconds() : resolution.absoluteSeconds();
        TenantSessionContext context = new TenantSessionContext(
                accountId,
                Long.parseLong(resolution.tenantId()),
                Long.parseLong(resolution.organizationId()),
                resolution.username(),
                resolution.displayName(),
                request.getAppCode(),
                rememberMe,
                request.getDevice().getDeviceId(),
                request.getDevice().getDeviceType(),
                request.getDevice().getDeviceName(),
                resolution.authzVersion(),
                Boolean.TRUE.equals(resolution.concurrentLoginEnabled()),
                resolution.maxDevices(),
                idleSeconds,
                absoluteSeconds
        );
        return LoginResultVO.authenticated(tenantSessionService.createPasswordSession(context, loginIp));
    }

    /** @param resolution IAM 快照 @return 有效租户账号 ID */
    private Long tenantAccountId(UsernameLoginResolutionInternalResponse resolution) {
        if (resolution == null || !resolution.resolved()
                || !AuthConstants.TENANT_ACCOUNT_SUBJECT.equals(resolution.subjectType())
                || !AuthConstants.ACTIVE_STATUS.equals(resolution.status())
                || resolution.subjectId() == null || resolution.tenantId() == null
                || resolution.organizationId() == null || resolution.authzVersion() == null
                || resolution.maxDevices() == null || resolution.idleSeconds() == null
                || resolution.absoluteSeconds() == null || resolution.rememberIdleSeconds() == null
                || resolution.rememberAbsoluteSeconds() == null) {
            return null;
        }
        try {
            return Long.parseLong(resolution.subjectId());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
