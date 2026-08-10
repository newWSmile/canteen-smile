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

/** 平台用户名密码登录和设备会话建立服务。 */
@Service
@RequiredArgsConstructor
public class PlatformPasswordLoginService {

    /** 用户名或密码错误码。 */
    private static final String INVALID_CREDENTIALS_CODE = "AUTH_1001";

    /** 应用入口不匹配错误码。 */
    private static final String APP_MISMATCH_CODE = "AUTH_1008";

    /** IAM 平台身份解析 Client。 */
    private final IamPlatformIdentityClient iamPlatformIdentityClient;

    /** 认证凭证数据访问接口。 */
    private final CredentialMapper credentialMapper;

    /** Argon2id 恒定时间密码验证服务。 */
    private final PasswordVerificationService passwordVerificationService;

    /** 登录失败保护服务。 */
    private final LoginProtectionService loginProtectionService;

    /** 平台设备会话服务。 */
    private final PlatformSessionService platformSessionService;

    /**
     * 验证用户名和 Argon2id 密码，成功后直接建立当前设备会话。
     *
     * @param request 密码登录请求
     * @param password 已从一次性信封中解密且仅在当前调用链使用的密码
     * @param loginIp 服务端解析的登录 IP
     * @return 已完成认证的登录结果
     */
    public LoginResultVO login(PasswordLoginRequest request, String password, String loginIp) {
        if (!AuthConstants.PLATFORM_ADMIN_APP.equals(request.getAppCode())) {
            throw new BusinessException(APP_MISMATCH_CODE, "当前应用入口不匹配", 403);
        }
        loginProtectionService.requirePasswordAttemptAllowed(
                request.getAppCode(),
                request.getUsername(),
                request.getCaptchaTicket()
        );
        /** IAM 返回的登录主体解析结果。 */
        UsernameLoginResolutionInternalResponse resolution = iamPlatformIdentityClient.resolveUsername(
                new UsernameLoginResolutionInternalRequest(request.getAppCode(), request.getUsername())
        );
        /** 解析到的平台身份 ID，无法解析时为空。 */
        Long platformIdentityId = resolvedPlatformIdentityId(resolution);
        /** Auth 本地密码凭证。 */
        CredentialEntity credential = platformIdentityId == null
                ? null
                : credentialMapper.selectBySubject(AuthConstants.PLATFORM_IDENTITY_SUBJECT, platformIdentityId);
        if (!passwordVerificationService.matches(password, credential)) {
            loginProtectionService.recordPasswordFailure(
                    request.getAppCode(),
                    request.getUsername(),
                    loginIp,
                    request.getDevice()
            );
            throw new BusinessException(INVALID_CREDENTIALS_CODE, "用户名或密码错误");
        }
        loginProtectionService.resetAfterSuccess(request.getAppCode(), request.getUsername());
        /** 已通过密码校验且不含任何秘密的会话创建上下文。 */
        PlatformSecondFactorContext context = new PlatformSecondFactorContext(
                platformIdentityId,
                resolution.username(),
                resolution.displayName(),
                request.getAppCode(),
                request.isRememberMe(),
                request.getDevice().getDeviceId(),
                request.getDevice().getDeviceType(),
                request.getDevice().getDeviceName(),
                resolution.authzVersion()
        );
        return LoginResultVO.authenticated(platformSessionService.createPasswordSession(context, loginIp));
    }

    /** @param resolution IAM 解析结果 @return 有效平台身份 ID，不符合时为空 */
    private Long resolvedPlatformIdentityId(UsernameLoginResolutionInternalResponse resolution) {
        if (resolution == null
                || !resolution.resolved()
                || !AuthConstants.PLATFORM_IDENTITY_SUBJECT.equals(resolution.subjectType())
                || !AuthConstants.ACTIVE_STATUS.equals(resolution.status())
                || resolution.subjectId() == null
                || resolution.authzVersion() == null) {
            return null;
        }
        try {
            return Long.parseLong(resolution.subjectId());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
