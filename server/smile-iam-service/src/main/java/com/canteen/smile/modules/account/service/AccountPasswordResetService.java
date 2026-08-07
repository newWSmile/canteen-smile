package com.canteen.smile.modules.account.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.mapper.AccountLifecycleMapper;
import com.canteen.smile.modules.account.vo.AccountActivationContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 租户账号密码恢复状态的本地事务服务。 */
@Service
@RequiredArgsConstructor
public class AccountPasswordResetService {

    /** 账号当前不可恢复错误码。 */
    private static final String ACCOUNT_NOT_RESETTABLE_CODE = "IAM_2304";

    /** 账号生命周期数据访问接口。 */
    private final AccountLifecycleMapper mapper;

    /**
     * 使账号进入待重置状态并提升授权版本。
     *
     * @param accountId 租户账号 ID
     * @param operatorId 发起平台身份 ID
     * @return 更新后的账号上下文
     */
    @Transactional
    public AccountActivationContextVO requirePasswordReset(long accountId, long operatorId) {
        AccountLifecycleMapper.ActivationContextRow row = requireAvailable(accountId);
        if (!("ACTIVE".equals(row.accountStatus()) || "PASSWORD_RESET_REQUIRED".equals(row.accountStatus()))) {
            throw unavailable();
        }
        if (mapper.markPasswordResetRequired(accountId, operatorId) != 1) {
            throw new BusinessException("IAM_2305", "账号密码恢复状态已变化", 409);
        }
        return toVO(requireAvailable(accountId));
    }

    /**
     * Auth 完成密码更新后幂等恢复 IAM 账号状态。
     *
     * @param accountId 租户账号 ID
     * @return 当前账号上下文
     */
    @Transactional
    public AccountActivationContextVO complete(long accountId) {
        AccountLifecycleMapper.ActivationContextRow row = requireAvailable(accountId);
        if ("PASSWORD_RESET_REQUIRED".equals(row.accountStatus())) {
            if (mapper.completePasswordReset(accountId) != 1) {
                throw new BusinessException("IAM_2305", "账号密码恢复状态已变化", 409);
            }
            row = requireAvailable(accountId);
        }
        if (!"ACTIVE".equals(row.accountStatus())) {
            throw unavailable();
        }
        return toVO(row);
    }

    /** @param accountId 租户账号 ID @return 可参与密码恢复的账号上下文 */
    private AccountLifecycleMapper.ActivationContextRow requireAvailable(long accountId) {
        AccountLifecycleMapper.ActivationContextRow row = mapper.selectActivationContext(accountId);
        if (row == null || !"ACTIVE".equals(row.tenantStatus())) {
            throw unavailable();
        }
        return row;
    }

    /** @param row 数据库账号上下文 @return 内部响应 */
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
        return new BusinessException(ACCOUNT_NOT_RESETTABLE_CODE, "账号不存在或当前不可恢复密码", 409);
    }
}
