package com.canteen.smile.modules.auth.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.parameter.enums.SaReplacedRange;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.modules.auth.entity.DeviceSessionEntity;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 按租户安全策略创建租户账号独立设备会话的服务。 */
@Service
@RequiredArgsConstructor
public class TenantSessionService {

    /** 设备会话持久化事务服务。 */
    private final DeviceSessionPersistenceService persistenceService;

    /** @param context 已验证租户会话上下文 @param loginIp 登录 IP @return 新设备会话 */
    public SessionVO createPasswordSession(TenantSessionContext context, String loginIp) {
        return create(context, loginIp, AuthConstants.PASSWORD_LOGIN_METHOD);
    }

    /** @param context 已验证租户会话上下文 @param loginIp 登录 IP @return 短信登录设备会话 */
    public SessionVO createSmsSession(TenantSessionContext context, String loginIp) {
        return create(context, loginIp, AuthConstants.SMS_LOGIN_METHOD);
    }

    /** @return 按指定认证方式创建并持久化的租户设备会话 */
    private SessionVO create(TenantSessionContext context, String loginIp, String loginMethod) {
        String loginId = AuthConstants.TENANT_LOGIN_PREFIX + context.accountId();
        int maxLoginCount = context.concurrentLoginEnabled() ? context.maxDevices() : 1;
        SaLoginParameter parameter = SaLoginParameter.create()
                .setDeviceType(context.deviceType())
                .setDeviceId(context.deviceId())
                .setTimeout(context.absoluteSeconds())
                .setActiveTimeout(context.idleSeconds())
                .setIsConcurrent(context.concurrentLoginEnabled())
                .setIsShare(false)
                .setMaxLoginCount(maxLoginCount)
                .setReplacedRange(SaReplacedRange.ALL_DEVICE_TYPE)
                .setRightNowCreateTokenSession(true);
        StpUtil.login(loginId, parameter);
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        String sessionId = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime idleExpiresAt = now.plusSeconds(context.idleSeconds());
        OffsetDateTime absoluteExpiresAt = now.plusSeconds(context.absoluteSeconds());
        SaSession tokenSession = StpUtil.getTokenSession();
        tokenSession.set("businessSessionId", sessionId);
        tokenSession.set(AuthConstants.TOKEN_APP_CODE_ATTRIBUTE, context.appCode());
        tokenSession.set("subjectType", AuthConstants.TENANT_ACCOUNT_SUBJECT);
        tokenSession.set(AuthConstants.TOKEN_TENANT_ID_ATTRIBUTE, context.tenantId());
        tokenSession.set(AuthConstants.TOKEN_ORGANIZATION_ID_ATTRIBUTE, context.organizationId());
        tokenSession.set("authzVersion", context.authzVersion());
        tokenSession.set(AuthConstants.TOKEN_USERNAME_ATTRIBUTE, context.username());
        tokenSession.set(
                AuthConstants.TOKEN_DISPLAY_NAME_ATTRIBUTE,
                context.displayName() == null ? context.username() : context.displayName()
        );
        try {
            persistenceService.create(
                    entity(
                            context, tokenInfo.getTokenValue(), sessionId, loginIp,
                            now, idleExpiresAt, absoluteExpiresAt, loginMethod
                    ),
                    context.username(),
                    context.displayName()
            );
        } catch (RuntimeException exception) {
            StpUtil.logoutByTokenValue(tokenInfo.getTokenValue());
            throw exception;
        }
        return new SessionVO(
                tokenInfo.getTokenName(), tokenInfo.getTokenValue(), sessionId, context.appCode(),
                AuthConstants.TENANT_ACCOUNT_SUBJECT, Long.toString(context.accountId()),
                Long.toString(context.tenantId()), Long.toString(context.organizationId()),
                idleExpiresAt, absoluteExpiresAt
        );
    }

    /** @return 待持久化的设备会话实体 */
    private DeviceSessionEntity entity(
            TenantSessionContext context,
            String tokenValue,
            String sessionId,
            String loginIp,
            OffsetDateTime now,
            OffsetDateTime idleExpiresAt,
            OffsetDateTime absoluteExpiresAt,
            String loginMethod
    ) {
        DeviceSessionEntity entity = new DeviceSessionEntity();
        entity.setSessionId(sessionId);
        entity.setSubjectType(AuthConstants.TENANT_ACCOUNT_SUBJECT);
        entity.setSubjectId(context.accountId());
        entity.setTenantId(context.tenantId());
        entity.setOrganizationId(context.organizationId());
        entity.setAppCode(context.appCode());
        entity.setTokenDigest(hash(tokenValue));
        entity.setDeviceIdHash(hash(context.deviceId()));
        entity.setDeviceType(context.deviceType());
        entity.setDeviceName(context.deviceName());
        entity.setLoginMethod(loginMethod);
        entity.setLoginIpMasked(maskIp(loginIp));
        entity.setLoginTime(now);
        entity.setLastActiveTime(now);
        entity.setIdleExpiresAt(idleExpiresAt);
        entity.setAbsoluteExpiresAt(absoluteExpiresAt);
        entity.setStatus(AuthConstants.ACTIVE_STATUS);
        entity.setSnapshotVersion(context.authzVersion());
        return entity;
    }

    /** @param value 原始值 @return SHA-256 摘要 */
    private String hash(String value) {
        return HmacRequestSigner.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    /** @param ipAddress 原始 IP @return 脱敏 IP */
    private String maskIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) return null;
        if (ipAddress.contains(".")) return ipAddress.replaceFirst("\\d+$", "*");
        String[] segments = ipAddress.split(":", -1);
        return segments.length > 4
                ? String.join(":", segments[0], segments[1], segments[2], segments[3]) + ":*"
                : "*";
    }
}
