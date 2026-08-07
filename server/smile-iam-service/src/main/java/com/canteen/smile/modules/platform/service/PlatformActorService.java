package com.canteen.smile.modules.platform.service;

import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.common.exception.BusinessException;
import org.springframework.stereotype.Service;

/** 从已认证 Sa-Token 上下文解析独立平台身份。 */
@Service
public class PlatformActorService {

    /** 平台登录 ID 前缀。 */
    private static final String PLATFORM_LOGIN_PREFIX = "PLATFORM:";

    /** @return 当前已认证平台身份 ID */
    public long currentPlatformIdentityId() {
        /** Sa-Token 当前登录 ID。 */
        String loginId = String.valueOf(StpUtil.getLoginId());
        if (!loginId.startsWith(PLATFORM_LOGIN_PREFIX)) {
            throw new BusinessException("IAM_2001", "当前身份不是平台身份", 403);
        }
        try {
            return Long.parseLong(loginId.substring(PLATFORM_LOGIN_PREFIX.length()));
        } catch (NumberFormatException exception) {
            throw new BusinessException("IAM_2001", "当前身份不是平台身份", 403);
        }
    }
}
