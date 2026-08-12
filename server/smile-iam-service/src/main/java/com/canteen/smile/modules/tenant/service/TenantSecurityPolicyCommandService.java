package com.canteen.smile.modules.tenant.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.tenant.dto.UpdateTenantSecurityPolicyRequest;
import com.canteen.smile.modules.tenant.entity.TenantSecurityPolicyEntity;
import com.canteen.smile.modules.tenant.mapper.TenantSecurityPolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在 IAM 本地事务中修改租户安全策略并产生必要的账号级会话失效事件。 */
@Service
@RequiredArgsConstructor
public class TenantSecurityPolicyCommandService {

    /** 租户安全策略数据访问接口。 */
    private final TenantSecurityPolicyMapper mapper;

    /**
     * 修改策略；只有收紧策略时才提升安全版本并使已有会话失效。
     *
     * @param actor 已完成数据库最终校验的租户根机构所有者
     * @param request 修改命令
     * @return 修改后的策略
     */
    @Transactional
    public TenantSecurityPolicyEntity update(
            TenantActorContext actor,
            UpdateTenantSecurityPolicyRequest request
    ) {
        TenantSecurityPolicyEntity current = requirePolicy(actor.tenantId());
        validate(request);
        boolean tightened = isTightened(current, request);
        if (mapper.updatePolicy(actor.tenantId(), request, actor.accountId()) != 1) {
            throw new BusinessException("IAM_2902", "安全策略已被其他操作修改，请刷新后重试", 409);
        }
        if (tightened) {
            if (mapper.bumpTenantSecurityVersion(actor.tenantId(), actor.accountId()) != 1) {
                throw new IllegalStateException("Tenant security version was not updated");
            }
            Long securityVersion = mapper.selectTenantSecurityVersion(actor.tenantId());
            if (securityVersion == null) {
                throw new IllegalStateException("Tenant security version is unavailable");
            }
            mapper.insertSecurityPolicyChangedEvents(actor.tenantId(), securityVersion, actor.accountId());
        }
        return requirePolicy(actor.tenantId());
    }

    /** 校验跨字段会话时长和密码到期参数。 */
    private void validate(UpdateTenantSecurityPolicyRequest request) {
        if (request.idleSeconds() > request.absoluteSeconds()) {
            throw new BusinessException("IAM_2903", "普通会话空闲时长不能超过最长存活时长", 400);
        }
        if (request.rememberIdleSeconds() > request.rememberAbsoluteSeconds()) {
            throw new BusinessException("IAM_2904", "记住我会话空闲时长不能超过最长存活时长", 400);
        }
        if (request.passwordExpiryEnabled() && request.passwordExpiryDays() == null) {
            throw new BusinessException("IAM_2905", "启用密码到期时必须设置密码有效天数", 400);
        }
        if (!request.passwordExpiryEnabled() && request.passwordExpiryDays() != null) {
            throw new BusinessException("IAM_2906", "关闭密码到期时不得保留密码有效天数", 400);
        }
    }

    /** 判断修改是否缩小现有账号安全边界。 */
    private boolean isTightened(
            TenantSecurityPolicyEntity current,
            UpdateTenantSecurityPolicyRequest request
    ) {
        return (current.getConcurrentLoginEnabled() && !request.concurrentLoginEnabled())
                || request.maxDevices() < current.getMaxDevices()
                || (current.getRememberMeEnabled() && !request.rememberMeEnabled())
                || request.idleSeconds() < current.getIdleSeconds()
                || request.absoluteSeconds() < current.getAbsoluteSeconds()
                || request.rememberIdleSeconds() < current.getRememberIdleSeconds()
                || request.rememberAbsoluteSeconds() < current.getRememberAbsoluteSeconds()
                || passwordPolicyTightened(current, request);
    }

    /** 判断密码有效期策略是否被收紧。 */
    private boolean passwordPolicyTightened(
            TenantSecurityPolicyEntity current,
            UpdateTenantSecurityPolicyRequest request
    ) {
        if (!current.getPasswordExpiryEnabled() && request.passwordExpiryEnabled()) {
            return true;
        }
        return current.getPasswordExpiryEnabled() && request.passwordExpiryEnabled()
                && request.passwordExpiryDays() < current.getPasswordExpiryDays();
    }

    /** @return 指定租户有效策略，不存在属于初始化数据损坏。 */
    private TenantSecurityPolicyEntity requirePolicy(long tenantId) {
        TenantSecurityPolicyEntity policy = mapper.selectByTenantId(tenantId);
        if (policy == null) {
            throw new BusinessException("IAM_2901", "当前租户安全策略不存在", 404);
        }
        return policy;
    }
}
