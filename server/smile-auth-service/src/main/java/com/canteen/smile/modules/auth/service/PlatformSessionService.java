package com.canteen.smile.modules.auth.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.parameter.enums.SaReplacedRange;
import com.canteen.smile.audit.model.AuditActor;
import com.canteen.smile.audit.model.AuditRecordCommand;
import com.canteen.smile.audit.service.AuditRecorder;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.modules.auth.entity.DeviceSessionEntity;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 平台身份 Sa-Token 独立设备会话编排服务。 */
@Service
@RequiredArgsConstructor
public class PlatformSessionService {

    /** 会话因安全变化失效错误码。 */
    private static final String SESSION_INVALIDATED_CODE = "AUTH_1006";

    /** 普通登录空闲期限秒数。 */
    private static final long NORMAL_IDLE_SECONDS = 2 * 60 * 60;

    /** 普通登录绝对期限秒数。 */
    private static final long NORMAL_ABSOLUTE_SECONDS = 7 * 24 * 60 * 60;

    /** 记住我空闲期限秒数。 */
    private static final long REMEMBER_IDLE_SECONDS = 7 * 24 * 60 * 60;

    /** 记住我绝对期限秒数。 */
    private static final long REMEMBER_ABSOLUTE_SECONDS = 30 * 24 * 60 * 60;

    /** 默认单账号最大有效设备数。 */
    private static final int DEFAULT_MAX_DEVICE_COUNT = 5;

    /** Token Session 中的业务会话 ID 属性名。 */
    private static final String SESSION_ID_ATTRIBUTE = "businessSessionId";

    /** Token Session 中的应用编码属性名。 */
    /** Token Session 中的主体类型属性名。 */
    private static final String SUBJECT_TYPE_ATTRIBUTE = "subjectType";

    /** Token Session 中的平台身份 ID 属性名。 */
    private static final String PLATFORM_IDENTITY_ID_ATTRIBUTE = "platformIdentityId";

    /** Token Session 中的授权版本属性名。 */
    private static final String AUTHZ_VERSION_ATTRIBUTE = "authzVersion";

    /** 设备会话数据库事务服务。 */
    private final DeviceSessionPersistenceService persistenceService;

    /** 无登录态认证流程使用的编程式审计记录器。 */
    private final AuditRecorder auditRecorder;

    /**
     * 为已经完成密码校验的平台身份创建独立设备会话。
     *
     * @param context 平台登录上下文
     * @param loginIp 服务端解析的登录 IP
     * @return 当前设备会话
     */
    public SessionVO createPasswordSession(PlatformSecondFactorContext context, String loginIp) {
        return createWithAudit(
                context,
                loginIp,
                AuthConstants.PASSWORD_LOGIN_METHOD,
                "auth:login:password",
                "用户名密码登录"
        );
    }

    /**
     * 为已经完成恢复码校验的平台身份创建独立设备会话。
     *
     * @param context 平台登录上下文
     * @param loginIp 服务端解析的登录 IP
     * @return 当前设备会话
     */
    public SessionVO createRecoveryCodeSession(PlatformSecondFactorContext context, String loginIp) {
        return createWithAudit(
                context,
                loginIp,
                AuthConstants.RECOVERY_CODE_LOGIN_METHOD,
                "auth:login:recovery-code",
                "恢复码登录"
        );
    }

    /** @return 创建平台会话并使用已验证身份上下文记录成功或失败审计 */
    private SessionVO createWithAudit(
            PlatformSecondFactorContext context,
            String loginIp,
            String loginMethod,
            String actionCode,
            String actionName
    ) {
        long startedNanos = System.nanoTime();
        AuditRecordCommand command = platformLoginAudit(
                context, loginMethod, actionCode, actionName
        );
        try {
            SessionVO session = create(context, loginIp, loginMethod);
            auditRecorder.recordSuccess(command, startedNanos);
            return session;
        } catch (RuntimeException exception) {
            auditRecorder.recordFailure(command, exception, startedNanos);
            throw exception;
        }
    }

    /** @return 只使用后端已验证平台身份构造的登录审计声明 */
    private AuditRecordCommand platformLoginAudit(
            PlatformSecondFactorContext context,
            String loginMethod,
            String actionCode,
            String actionName
    ) {
        String displayName = context.displayName() == null
                ? context.username() : context.displayName();
        return AuditRecordCommand.builder()
                .source("AUTH")
                .categoryPath("平台管理端", "认证安全", "登录")
                .actionCode(actionCode)
                .actionName(actionName)
                .targetType(AuthConstants.PLATFORM_IDENTITY_SUBJECT)
                .targetId(context.platformIdentityId())
                .targetName(displayName)
                .targetCode(context.username())
                .loginMethod(loginMethod)
                .deviceSummary(deviceSummary(context.deviceType(), context.deviceName()))
                .actor(new AuditActor(
                        null,
                        AuthConstants.PLATFORM_IDENTITY_SUBJECT,
                        context.platformIdentityId(),
                        null,
                        context.username(),
                        displayName,
                        context.appCode()
                ))
                .build();
    }

    /** @return 不包含设备标识的脱敏设备类型和名称摘要 */
    private String deviceSummary(String deviceType, String deviceName) {
        return deviceName == null || deviceName.isBlank()
                ? deviceType : deviceType + " / " + deviceName;
    }

    /**
     * 根据已确认登录方式创建平台设备会话。
     *
     * @param context 平台登录上下文
     * @param loginIp 服务端解析的登录 IP
     * @param loginMethod 已确认登录方式
     * @return 当前设备会话
     */
    private SessionVO create(
            PlatformSecondFactorContext context,
            String loginIp,
            String loginMethod
    ) {
        /** 会话空闲期限秒数。 */
        long idleSeconds = context.rememberMe() ? REMEMBER_IDLE_SECONDS : NORMAL_IDLE_SECONDS;
        /** 会话绝对期限秒数。 */
        long absoluteSeconds = context.rememberMe() ? REMEMBER_ABSOLUTE_SECONDS : NORMAL_ABSOLUTE_SECONDS;
        /** 平台身份 Sa-Token 登录 ID。 */
        String loginId = AuthConstants.PLATFORM_LOGIN_PREFIX + context.platformIdentityId();
        /** 每次设备登录使用的 Sa-Token 参数。 */
        SaLoginParameter loginParameter = SaLoginParameter.create()
                .setDeviceType(context.deviceType())
                .setDeviceId(context.deviceId())
                .setTimeout(absoluteSeconds)
                .setActiveTimeout(idleSeconds)
                .setIsConcurrent(true)
                .setIsShare(false)
                .setMaxLoginCount(DEFAULT_MAX_DEVICE_COUNT)
                .setReplacedRange(SaReplacedRange.ALL_DEVICE_TYPE)
                .setRightNowCreateTokenSession(true);
        StpUtil.login(loginId, loginParameter);
        /** Sa-Token 当前登录结果。 */
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        /** 当前业务设备会话 ID。 */
        String sessionId = UUID.randomUUID().toString();
        /** 当前时间。 */
        OffsetDateTime now = OffsetDateTime.now();
        /** 当前设备空闲失效时间。 */
        OffsetDateTime idleExpiresAt = now.plusSeconds(idleSeconds);
        /** 当前设备绝对失效时间。 */
        OffsetDateTime absoluteExpiresAt = now.plusSeconds(absoluteSeconds);

        /** 当前 Token 独立会话。 */
        SaSession tokenSession = StpUtil.getTokenSession();
        tokenSession.set(SESSION_ID_ATTRIBUTE, sessionId);
        tokenSession.set(AuthConstants.TOKEN_APP_CODE_ATTRIBUTE, context.appCode());
        tokenSession.set(SUBJECT_TYPE_ATTRIBUTE, AuthConstants.PLATFORM_IDENTITY_SUBJECT);
        tokenSession.set(PLATFORM_IDENTITY_ID_ATTRIBUTE, context.platformIdentityId());
        tokenSession.set(AUTHZ_VERSION_ATTRIBUTE, context.authzVersion());
        tokenSession.set(AuthConstants.TOKEN_USERNAME_ATTRIBUTE, context.username());
        tokenSession.set(
                AuthConstants.TOKEN_DISPLAY_NAME_ATTRIBUTE,
                context.displayName() == null ? context.username() : context.displayName()
        );

        try {
            persistenceService.create(toEntity(
                    context,
                    tokenInfo.getTokenValue(),
                    sessionId,
                    loginMethod,
                    loginIp,
                    now,
                    idleExpiresAt,
                    absoluteExpiresAt
            ));
        } catch (RuntimeException exception) {
            StpUtil.logoutByTokenValue(tokenInfo.getTokenValue());
            throw exception;
        }
        return toVO(tokenInfo, sessionId, context, idleExpiresAt, absoluteExpiresAt);
    }

    /** @return 当前设备会话 */
    public SessionVO current() {
        /** 当前 Token 独立会话。 */
        SaSession tokenSession = StpUtil.getTokenSession();
        /** 当前业务会话 ID。 */
        Object sessionIdValue = tokenSession.get(SESSION_ID_ATTRIBUTE);
        if (sessionIdValue == null) {
            invalidateCurrentSession();
        }
        /** 数据库中的有效设备会话。 */
        DeviceSessionEntity entity = persistenceService.findActive(String.valueOf(sessionIdValue));
        if (entity == null || entity.getAbsoluteExpiresAt().isBefore(OffsetDateTime.now())) {
            invalidateCurrentSession();
        }
        /** 当前 Sa-Token 信息。 */
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return new SessionVO(
                tokenInfo.getTokenName(),
                tokenInfo.getTokenValue(),
                entity.getSessionId(),
                entity.getAppCode(),
                entity.getSubjectType(),
                entity.getSubjectId().toString(),
                entity.getTenantId() == null ? null : entity.getTenantId().toString(),
                entity.getOrganizationId() == null ? null : entity.getOrganizationId().toString(),
                entity.getIdleExpiresAt(),
                entity.getAbsoluteExpiresAt()
        );
    }

    /** 退出当前设备会话。 */
    public void logoutCurrent() {
        /** 当前 Token 独立会话。 */
        SaSession tokenSession = StpUtil.getTokenSession();
        /** 当前业务会话 ID。 */
        Object sessionId = tokenSession.get(SESSION_ID_ATTRIBUTE);
        if (sessionId != null) {
            persistenceService.markLoggedOut(String.valueOf(sessionId));
        }
        StpUtil.logout();
    }

    /** 使当前不完整或数据库已失效的会话退出并抛出稳定错误。 */
    private void invalidateCurrentSession() {
        StpUtil.logout();
        throw new BusinessException(SESSION_INVALIDATED_CODE, "会话已因安全变化失效", 401);
    }

    /**
     * 将已确认登录上下文转换为数据库会话实体。
     *
     * @param context 平台登录上下文
     * @param tokenValue Sa-Token 明文，仅用于计算摘要
     * @param sessionId 业务设备会话 ID
     * @param loginMethod 已确认登录方式
     * @param loginIp 服务端解析的登录 IP
     * @param now 登录时间
     * @param idleExpiresAt 空闲失效时间
     * @param absoluteExpiresAt 绝对失效时间
     * @return 设备会话实体
     */
    private DeviceSessionEntity toEntity(
            PlatformSecondFactorContext context,
            String tokenValue,
            String sessionId,
            String loginMethod,
            String loginIp,
            OffsetDateTime now,
            OffsetDateTime idleExpiresAt,
            OffsetDateTime absoluteExpiresAt
    ) {
        /** 新建设备会话实体。 */
        DeviceSessionEntity entity = new DeviceSessionEntity();
        entity.setSessionId(sessionId);
        entity.setSubjectType(AuthConstants.PLATFORM_IDENTITY_SUBJECT);
        entity.setSubjectId(context.platformIdentityId());
        entity.setAppCode(context.appCode());
        entity.setTokenDigest(sha256(tokenValue));
        entity.setDeviceIdHash(sha256(context.deviceId()));
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

    /** 将新会话转换成外部响应。 */
    private SessionVO toVO(
            SaTokenInfo tokenInfo,
            String sessionId,
            PlatformSecondFactorContext context,
            OffsetDateTime idleExpiresAt,
            OffsetDateTime absoluteExpiresAt
    ) {
        return new SessionVO(
                tokenInfo.getTokenName(),
                tokenInfo.getTokenValue(),
                sessionId,
                context.appCode(),
                AuthConstants.PLATFORM_IDENTITY_SUBJECT,
                Long.toString(context.platformIdentityId()),
                null,
                null,
                idleExpiresAt,
                absoluteExpiresAt
        );
    }

    /** @param value 原始值 @return SHA-256 小写十六进制摘要 */
    private String sha256(String value) {
        return HmacRequestSigner.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    /** @param ipAddress 来源 IP @return 不暴露完整地址的展示值 */
    private String maskIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }
        if (ipAddress.contains(".")) {
            return ipAddress.replaceFirst("\\d+$", "*");
        }
        /** IPv6 地址片段。 */
        String[] segments = ipAddress.split(":", -1);
        if (segments.length > 4) {
            return String.join(":", segments[0], segments[1], segments[2], segments[3]) + ":*";
        }
        return "*";
    }
}
