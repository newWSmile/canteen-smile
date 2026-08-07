package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.entity.PlatformRecoveryCodeEntity;
import com.canteen.smile.modules.auth.mapper.PlatformRecoveryCodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 平台恢复码加锁校验和一次性消费服务。 */
@Service
@RequiredArgsConstructor
public class PlatformRecoveryCodeService {

    /** 一次性票据或恢复码无效错误码。 */
    private static final String INVALID_RECOVERY_CODE = "AUTH_1007";

    /** 恢复码摘要组件。 */
    private final RecoveryCodeGenerator recoveryCodeGenerator;

    /** 平台恢复码数据访问接口。 */
    private final PlatformRecoveryCodeMapper platformRecoveryCodeMapper;

    /**
     * 加锁并消费一枚有效恢复码。
     *
     * @param platformIdentityId 平台身份 ID
     * @param recoveryCode 用户提交的恢复码
     */
    @Transactional
    public void consume(long platformIdentityId, String recoveryCode) {
        /** 以规范输入计算的恢复码摘要。 */
        String codeHash = recoveryCodeGenerator.hash(recoveryCode);
        /** 加锁查询到的有效恢复码。 */
        PlatformRecoveryCodeEntity entity = platformRecoveryCodeMapper.selectActiveForUpdate(
                platformIdentityId,
                codeHash
        );
        if (entity == null
                || platformRecoveryCodeMapper.consumeRecoveryCode(entity.getId(), entity.getVersion()) != 1) {
            throw new BusinessException(INVALID_RECOVERY_CODE, "一次性票据无效或已使用");
        }
    }
}
