package com.canteen.smile.modules.account.service;

import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.mapper.AccountLifecycleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 从 Sa-Token 登录 ID 和 IAM 数据库解析有效租户操作人。 */
@Service
@RequiredArgsConstructor
public class TenantActorService {

    /** 租户账号登录 ID 前缀。 */
    private static final String TENANT_LOGIN_PREFIX = "TENANT:";

    /** 租户账号生命周期数据访问接口。 */
    private final AccountLifecycleMapper mapper;

    /** @return 当前有效租户操作人 */
    @Transactional(readOnly = true)
    public TenantActorContext current() {
        /** 当前 Sa-Token 登录 ID。 */
        String loginId = String.valueOf(StpUtil.getLoginId());
        if (!loginId.startsWith(TENANT_LOGIN_PREFIX)) {
            throw forbidden();
        }
        /** 当前租户账号 ID。 */
        long accountId;
        try {
            accountId = Long.parseLong(loginId.substring(TENANT_LOGIN_PREFIX.length()));
        } catch (NumberFormatException exception) {
            throw forbidden();
        }
        AccountLifecycleMapper.TenantPermissionContextRow row = mapper.selectTenantPermissionContext(accountId);
        if (row == null
                || !"ACTIVE".equals(row.accountStatus())
                || !"ACTIVE".equals(row.tenantStatus())
                || !"ACTIVE".equals(row.organizationStatus())) {
            throw forbidden();
        }
        return new TenantActorContext(
                row.accountId(),
                row.tenantId(),
                row.tenantName(),
                row.organizationId(),
                row.organizationName(),
                row.rootOrganizationId(),
                row.username(),
                row.displayName() == null ? row.username() : row.displayName(),
                row.organizationOwner(),
                row.rootOwner()
        );
    }

    /** @return 无效租户身份异常 */
    private BusinessException forbidden() {
        return new BusinessException("IAM_2401", "当前租户身份无效或已失去访问资格", 403);
    }
}
