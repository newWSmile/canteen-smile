package com.canteen.smile.modules.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;
import com.canteen.smile.modules.auth.model.ReauthAction;
import com.canteen.smile.modules.auth.vo.ReauthTicketVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 当前平台或租户身份通过当前密码获得五分钟敏感操作再认证票据。 */
@Service
@RequiredArgsConstructor
public class PlatformPasswordReauthService {

    /** 再认证失败稳定错误码。 */
    private static final String REAUTH_FAILED_CODE = "AUTH_1201";

    /** 认证凭证数据访问接口。 */
    private final CredentialMapper credentialMapper;

    /** 恒定时间密码校验服务。 */
    private final PasswordVerificationService passwordVerificationService;

    /** 统一再认证票据签发服务。 */
    private final ReauthTicketIssueService ticketIssueService;

    /**
     * 校验当前平台身份密码并签发只允许单个操作的一次性票据。
     *
     * @param rawPassword 当前请求解密出的密码
     * @param allowedAction 票据允许执行的敏感操作
     * @return 原始票据及失效时间
     */
    public ReauthTicketVO issue(String rawPassword, ReauthAction allowedAction) {
        /** 当前登录身份。 */
        ReauthSubject subject = currentSubject();
        validateAllowedAction(subject.subjectType(), allowedAction);
        /** 当前身份凭证。 */
        CredentialEntity credential = credentialMapper.selectBySubject(
                subject.subjectType(),
                subject.subjectId()
        );
        if (!passwordVerificationService.matches(rawPassword, credential)) {
            throw new BusinessException(REAUTH_FAILED_CODE, "当前密码验证失败", 401);
        }
        return ticketIssueService.issue(
                subject.subjectType(),
                subject.subjectId(),
                allowedAction,
                AuthConstants.PASSWORD_LOGIN_METHOD
        );
    }

    /** @return 当前登录身份对应的密码信封用途 */
    public PasswordEnvelopePurpose currentPasswordPurpose() {
        return AuthConstants.PLATFORM_IDENTITY_SUBJECT.equals(currentSubject().subjectType())
                ? PasswordEnvelopePurpose.PLATFORM_REAUTH_PASSWORD
                : PasswordEnvelopePurpose.TENANT_REAUTH_PASSWORD;
    }

    /** @return 当前 Sa-Token 中的平台或租户身份 */
    private ReauthSubject currentSubject() {
        /** 当前登录 ID。 */
        String loginId = String.valueOf(StpUtil.getLoginId());
        String subjectType;
        String subjectIdText;
        if (loginId.startsWith(AuthConstants.PLATFORM_LOGIN_PREFIX)) {
            subjectType = AuthConstants.PLATFORM_IDENTITY_SUBJECT;
            subjectIdText = loginId.substring(AuthConstants.PLATFORM_LOGIN_PREFIX.length());
        } else if (loginId.startsWith(AuthConstants.TENANT_LOGIN_PREFIX)) {
            subjectType = AuthConstants.TENANT_ACCOUNT_SUBJECT;
            subjectIdText = loginId.substring(AuthConstants.TENANT_LOGIN_PREFIX.length());
        } else {
            throw new BusinessException("AUTH_1202", "当前身份不支持密码再认证", 403);
        }
        try {
            return new ReauthSubject(subjectType, Long.parseLong(subjectIdText));
        } catch (NumberFormatException exception) {
            throw new BusinessException("AUTH_1202", "当前身份不支持密码再认证", 403);
        }
    }

    /** 校验身份类型只能签发自身允许的敏感动作。 */
    private void validateAllowedAction(String subjectType, ReauthAction action) {
        boolean platformAction = action == ReauthAction.TENANT_OWNER_PASSWORD_RESET
                || action == ReauthAction.PLATFORM_TENANT_GOVERNANCE
                || action == ReauthAction.PLATFORM_SMS_POLICY_UPDATE;
        if (platformAction != AuthConstants.PLATFORM_IDENTITY_SUBJECT.equals(subjectType)) {
            throw new BusinessException("AUTH_1202", "当前身份不能执行该敏感操作", 403);
        }
    }

    /** 当前再认证主体。 */
    private record ReauthSubject(String subjectType, long subjectId) {
    }
}
