package com.canteen.smile.modules.auth.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.model.AuthConstants;
import org.springframework.stereotype.Service;

/** 从服务端 Sa-Token 会话读取当前可信认证主体，禁止由前端提交主体 ID。 */
@Service
public class CurrentAuthSubjectService {

    /** 当前身份不支持租户账号个人安全操作的错误码。 */
    private static final String UNSUPPORTED_SUBJECT_CODE = "AUTH_1202";

    /** @return 当前已经登录的租户账号安全上下文 */
    public CurrentTenantSubject currentTenant() {
        /** Sa-Token 当前登录 ID。 */
        String loginId = String.valueOf(StpUtil.getLoginId());
        if (!loginId.startsWith(AuthConstants.TENANT_LOGIN_PREFIX)) {
            throw unsupportedSubject();
        }
        /** 登录 ID 中受服务端控制的账号 ID 文本。 */
        String accountIdText = loginId.substring(AuthConstants.TENANT_LOGIN_PREFIX.length());
        /** 当前独立 Token Session。 */
        SaSession tokenSession = StpUtil.getTokenSession();
        /** Token Session 中的租户 ID。 */
        Object tenantIdValue = tokenSession.get(AuthConstants.TOKEN_TENANT_ID_ATTRIBUTE);
        try {
            long accountId = Long.parseLong(accountIdText);
            long tenantId = Long.parseLong(String.valueOf(tenantIdValue));
            return new CurrentTenantSubject(
                    accountId,
                    tenantId,
                    nullableText(tokenSession.get(AuthConstants.TOKEN_USERNAME_ATTRIBUTE)),
                    nullableText(tokenSession.get(AuthConstants.TOKEN_DISPLAY_NAME_ATTRIBUTE))
            );
        } catch (NumberFormatException exception) {
            throw unsupportedSubject();
        }
    }

    /** @param value 会话属性 @return 去除空白后的可选文本 */
    private String nullableText(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    /** @return 不泄露会话内部结构的稳定业务异常 */
    private BusinessException unsupportedSubject() {
        return new BusinessException(UNSUPPORTED_SUBJECT_CODE, "当前身份不能执行该账号安全操作", 403);
    }

    /**
     * 当前租户账号安全上下文。
     *
     * @param accountId 租户账号 ID
     * @param tenantId 租户 ID
     * @param username 当前用户名快照；旧会话可能为空
     * @param displayName 当前显示名称快照；旧会话可能为空
     */
    public record CurrentTenantSubject(
            long accountId,
            long tenantId,
            String username,
            String displayName
    ) {
    }
}
