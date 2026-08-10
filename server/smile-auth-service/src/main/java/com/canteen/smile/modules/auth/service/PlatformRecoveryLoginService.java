package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.IamPlatformIdentityClient;
import com.canteen.smile.internal.client.dto.UsernameLoginResolutionInternalRequest;
import com.canteen.smile.internal.client.dto.UsernameLoginResolutionInternalResponse;
import com.canteen.smile.modules.auth.dto.PlatformRecoveryLoginRequest;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 平台恢复码二次验证和设备会话建立服务。 */
@Service
@RequiredArgsConstructor
public class PlatformRecoveryLoginService {

    /** 一次性票据无效错误码。 */
    private static final String INVALID_TICKET_CODE = "AUTH_1007";

    /** 平台二次验证票据服务。 */
    private final PlatformSecondFactorTicketService ticketService;

    /** IAM 平台身份解析 Client。 */
    private final IamPlatformIdentityClient iamPlatformIdentityClient;

    /** 平台恢复码一次性消费服务。 */
    private final PlatformRecoveryCodeService recoveryCodeService;

    /** 平台设备会话服务。 */
    private final PlatformSessionService platformSessionService;

    /**
     * 使用恢复码替代短信验证码完成平台二次验证。
     *
     * @param request 恢复码登录请求
     * @param loginIp 服务端解析的登录 IP
     * @return 当前设备会话
     */
    public SessionVO login(PlatformRecoveryLoginRequest request, String loginIp) {
        /** 原子消费后的二次验证上下文。 */
        PlatformSecondFactorContext context = ticketService.consume(request.getSecondFactorTicket());
        /** 会话签发前重新查询的 IAM 主体状态。 */
        UsernameLoginResolutionInternalResponse resolution = iamPlatformIdentityClient.resolveUsername(
                new UsernameLoginResolutionInternalRequest(context.appCode(), context.username())
        );
        if (!sameActiveIdentity(context, resolution)) {
            throw new BusinessException(INVALID_TICKET_CODE, "一次性票据无效或已使用");
        }
        recoveryCodeService.consume(context.platformIdentityId(), request.getRecoveryCode());
        /** 使用 IAM 最新授权版本建立会话。 */
        PlatformSecondFactorContext currentContext = new PlatformSecondFactorContext(
                context.platformIdentityId(),
                resolution.username(),
                resolution.displayName(),
                context.appCode(),
                context.rememberMe(),
                context.deviceId(),
                context.deviceType(),
                context.deviceName(),
                resolution.authzVersion()
        );
        return platformSessionService.createRecoveryCodeSession(currentContext, loginIp);
    }

    /** @return IAM 主体是否仍是同一个有效平台身份 */
    private boolean sameActiveIdentity(
            PlatformSecondFactorContext context,
            UsernameLoginResolutionInternalResponse resolution
    ) {
        return resolution != null
                && resolution.resolved()
                && AuthConstants.PLATFORM_IDENTITY_SUBJECT.equals(resolution.subjectType())
                && AuthConstants.ACTIVE_STATUS.equals(resolution.status())
                && Long.toString(context.platformIdentityId()).equals(resolution.subjectId())
                && resolution.authzVersion() != null;
    }
}
