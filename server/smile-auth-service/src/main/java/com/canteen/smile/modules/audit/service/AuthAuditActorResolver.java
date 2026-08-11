package com.canteen.smile.modules.audit.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.audit.model.AuditActor;
import com.canteen.smile.audit.spi.AuditActorResolver;
import com.canteen.smile.modules.auth.model.AuthConstants;
import org.springframework.stereotype.Component;

/** 从 Auth 当前可信 Sa-Token 会话捕获异步审计所需的登录人快照。 */
@Component
public class AuthAuditActorResolver implements AuditActorResolver {

    /** Sa-Token 登录 ID 无法解析时使用的明确系统身份。 */
    private static final AuditActor SYSTEM_ACTOR = AuditActor.system();

    /** @return 当前业务线程中的已登录平台身份、租户账号或明确系统身份 */
    @Override
    public AuditActor resolve() {
        if (!StpUtil.isLogin()) {
            return SYSTEM_ACTOR;
        }
        String loginId = String.valueOf(StpUtil.getLoginId());
        SaSession tokenSession = StpUtil.getTokenSession();
        if (loginId.startsWith(AuthConstants.TENANT_LOGIN_PREFIX)) {
            return tenantActor(loginId, tokenSession);
        }
        if (loginId.startsWith(AuthConstants.PLATFORM_LOGIN_PREFIX)) {
            return platformActor(loginId, tokenSession);
        }
        return SYSTEM_ACTOR;
    }

    /** 从服务端生成的租户登录 ID 和 Token Session 构造租户账号快照。 */
    private AuditActor tenantActor(String loginId, SaSession tokenSession) {
        Long accountId = parseId(loginId, AuthConstants.TENANT_LOGIN_PREFIX);
        Long tenantId = longValue(tokenSession.get(AuthConstants.TOKEN_TENANT_ID_ATTRIBUTE));
        if (accountId == null || tenantId == null) {
            return SYSTEM_ACTOR;
        }
        return new AuditActor(
                tenantId,
                AuthConstants.TENANT_ACCOUNT_SUBJECT,
                accountId,
                longValue(tokenSession.get(AuthConstants.TOKEN_ORGANIZATION_ID_ATTRIBUTE)),
                text(tokenSession.get(AuthConstants.TOKEN_USERNAME_ATTRIBUTE)),
                text(tokenSession.get(AuthConstants.TOKEN_DISPLAY_NAME_ATTRIBUTE)),
                text(tokenSession.get(AuthConstants.TOKEN_APP_CODE_ATTRIBUTE))
        );
    }

    /** 从服务端生成的平台登录 ID 和 Token Session 构造平台身份快照。 */
    private AuditActor platformActor(String loginId, SaSession tokenSession) {
        Long identityId = parseId(loginId, AuthConstants.PLATFORM_LOGIN_PREFIX);
        if (identityId == null) {
            return SYSTEM_ACTOR;
        }
        return new AuditActor(
                null,
                AuthConstants.PLATFORM_IDENTITY_SUBJECT,
                identityId,
                null,
                text(tokenSession.get(AuthConstants.TOKEN_USERNAME_ATTRIBUTE)),
                text(tokenSession.get(AuthConstants.TOKEN_DISPLAY_NAME_ATTRIBUTE)),
                text(tokenSession.get(AuthConstants.TOKEN_APP_CODE_ATTRIBUTE))
        );
    }

    /** @return 从固定前缀登录 ID 中解析出的正数身份 ID，格式无效时为空 */
    private Long parseId(String loginId, String prefix) {
        try {
            long value = Long.parseLong(loginId.substring(prefix.length()));
            return value > 0 ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** @return 会话数字属性的长整型值，缺失或格式无效时为空 */
    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** @return 去除首尾空白的会话文本，缺失或空白时为空 */
    private String text(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value).strip();
    }
}
