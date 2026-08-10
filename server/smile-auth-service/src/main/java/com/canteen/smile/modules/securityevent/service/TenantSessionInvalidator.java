package com.canteen.smile.modules.securityevent.service;

import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.modules.auth.model.AuthConstants;
import org.springframework.stereotype.Component;

/** 隔离 Sa-Token 静态 API 的租户账号全设备会话失效器。 */
@Component
public class TenantSessionInvalidator {

    /** 让指定租户账号在全部设备上的 Sa-Token 会话立即失效。 */
    public void invalidateAll(long accountId) {
        StpUtil.logout(AuthConstants.TENANT_LOGIN_PREFIX + accountId);
    }
}
