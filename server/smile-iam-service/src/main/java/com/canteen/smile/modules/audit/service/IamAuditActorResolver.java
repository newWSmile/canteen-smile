package com.canteen.smile.modules.audit.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.audit.model.AuditActor;
import com.canteen.smile.audit.spi.AuditActorResolver;
import com.canteen.smile.modules.platform.entity.PlatformIdentityEntity;
import com.canteen.smile.modules.platform.mapper.PlatformIdentityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 从 IAM 当前 Sa-Token 会话捕获操作人，并为旧平台会话补齐身份名称快照。 */
@Component
@RequiredArgsConstructor
public class IamAuditActorResolver implements AuditActorResolver {

    /** 平台身份登录 ID 前缀。 */
    private static final String PLATFORM_PREFIX = "PLATFORM:";

    /** 租户账号登录 ID 前缀。 */
    private static final String TENANT_PREFIX = "TENANT:";

    /** 平台身份查询接口，仅用于旧会话缺少名称快照时补齐。 */
    private final PlatformIdentityMapper platformIdentityMapper;

    /** @return 当前业务线程内的登录人不可变快照 */
    @Override
    public AuditActor resolve() {
        if (!StpUtil.isLogin()) {
            return AuditActor.system();
        }
        String loginId = String.valueOf(StpUtil.getLoginId());
        SaSession tokenSession = StpUtil.getTokenSession();
        if (loginId.startsWith(TENANT_PREFIX)) {
            return tenantActor(loginId, tokenSession);
        }
        if (loginId.startsWith(PLATFORM_PREFIX)) {
            return platformActor(loginId, tokenSession);
        }
        return AuditActor.system();
    }

    /** 构造租户账号操作人快照，字段全部来自 Auth 创建的服务端会话。 */
    private AuditActor tenantActor(String loginId, SaSession tokenSession) {
        Long accountId = parseId(loginId, TENANT_PREFIX);
        Long tenantId = longValue(tokenSession.get("tenantId"));
        if (accountId == null || tenantId == null) {
            return AuditActor.system();
        }
        return new AuditActor(
                tenantId,
                "TENANT_ACCOUNT",
                accountId,
                longValue(tokenSession.get("organizationId")),
                text(tokenSession.get("username")),
                text(tokenSession.get("displayName")),
                text(tokenSession.get("appCode"))
        );
    }

    /** 构造平台身份操作人快照，旧会话缺少名称时从 IAM 自有身份表补齐。 */
    private AuditActor platformActor(String loginId, SaSession tokenSession) {
        Long identityId = parseId(loginId, PLATFORM_PREFIX);
        if (identityId == null) {
            return AuditActor.system();
        }
        String username = text(tokenSession.get("username"));
        String displayName = text(tokenSession.get("displayName"));
        if (username == null || displayName == null) {
            PlatformIdentityEntity identity = platformIdentityMapper.selectById(identityId);
            if (identity != null) {
                username = username == null ? identity.getUsername() : username;
                displayName = displayName == null
                        ? firstText(identity.getDisplayName(), identity.getUsername())
                        : displayName;
            }
        }
        return new AuditActor(
                null,
                "PLATFORM_IDENTITY",
                identityId,
                null,
                username,
                displayName,
                text(tokenSession.get("appCode"))
        );
    }

    /** @return 固定前缀登录 ID 中的正数身份 ID，格式无效时为空 */
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

    /** @return 第一个非空白文本 */
    private String firstText(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
