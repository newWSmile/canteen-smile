package com.canteen.smile.modules.account.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.mapper.AccountLifecycleMapper;
import com.canteen.smile.modules.account.vo.AccountActivationContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 租户账号激活上下文查询和状态同步服务。 */
@Service
@RequiredArgsConstructor
public class AccountActivationService {

    /** 账号不存在或不可激活错误码。 */
    private static final String ACCOUNT_NOT_ACTIVATABLE_CODE = "IAM_2301";

    /** 租户账号生命周期数据访问接口。 */
    private final AccountLifecycleMapper mapper;

    /** @param accountId 账号 ID @return 可展示激活上下文 */
    @Transactional(readOnly = true)
    public AccountActivationContextVO context(long accountId) {
        AccountLifecycleMapper.ActivationContextRow row = mapper.selectActivationContext(accountId);
        if (row == null || !"ACTIVE".equals(row.tenantStatus())) {
            throw unavailable();
        }
        return toVO(row);
    }

    /** @param accountId 账号 ID @return 激活后的账号上下文 */
    @Transactional
    public AccountActivationContextVO activate(long accountId) {
        AccountLifecycleMapper.ActivationContextRow row = mapper.selectActivationContext(accountId);
        if (row == null || !"ACTIVE".equals(row.tenantStatus())) {
            throw unavailable();
        }
        if ("PENDING_ACTIVATION".equals(row.accountStatus())) {
            if (mapper.activatePendingAccount(accountId) != 1) {
                throw new BusinessException("IAM_2302", "账号激活状态已变化", 409);
            }
            row = mapper.selectActivationContext(accountId);
        }
        if (row == null || !"ACTIVE".equals(row.accountStatus())) {
            throw unavailable();
        }
        return toVO(row);
    }

    /** @param row 数据库激活上下文 @return 内部响应 */
    private AccountActivationContextVO toVO(AccountLifecycleMapper.ActivationContextRow row) {
        return new AccountActivationContextVO(
                Long.toString(row.accountId()),
                Long.toString(row.tenantId()),
                Long.toString(row.organizationId()),
                row.username(),
                row.displayName() == null ? row.username() : row.displayName(),
                row.tenantName(),
                row.organizationName(),
                row.accountStatus()
        );
    }

    /** @return 不泄露账号存在性的统一异常 */
    private BusinessException unavailable() {
        return new BusinessException(ACCOUNT_NOT_ACTIVATABLE_CODE, "账号不存在或当前不可激活", 404);
    }
}
