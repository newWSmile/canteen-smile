package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.config.AuthSecurityConfiguration;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.entity.PlatformRecoveryCodeEntity;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import com.canteen.smile.modules.auth.mapper.PlatformRecoveryCodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 平台凭证和恢复码本地事务服务。 */
@Service
@RequiredArgsConstructor
public class CredentialProvisionService {

    /** 首位平台管理员引导已关闭错误码。 */
    private static final String BOOTSTRAP_CLOSED_CODE = "AUTH_1010";

    /** 认证主体类型。 */
    private static final String PLATFORM_IDENTITY = "PLATFORM_IDENTITY";

    /** 凭证数据访问接口。 */
    private final CredentialMapper credentialMapper;

    /** 平台恢复码数据访问接口。 */
    private final PlatformRecoveryCodeMapper recoveryCodeMapper;

    /** 恢复码生成器。 */
    private final RecoveryCodeGenerator recoveryCodeGenerator;

    /** Argon2id 密码编码器。 */
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建或恢复未完成的首位平台凭证，并生成新的恢复码批次。
     * Argon2id 摘要必须在进入本事务前计算，避免长时间占用数据库事务。
     *
     * @param platformIdentityId 平台身份 ID
     * @param rawPassword 原始密码，仅用于未完成流程的凭证一致性校验
     * @param encodedPassword 已计算的 Argon2id 摘要
     * @param recoveryCodeCount 恢复码数量
     * @return 仅允许向调用方展示一次的恢复码
     */
    @Transactional
    public List<String> provision(
            long platformIdentityId,
            String rawPassword,
            String encodedPassword,
            int recoveryCodeCount
    ) {
        /** 当前平台身份凭证。 */
        CredentialEntity existing = credentialMapper.selectBySubject(PLATFORM_IDENTITY, platformIdentityId);
        /** 平台凭证总数。 */
        long platformCredentialCount = credentialMapper.countPlatformCredentials();
        if (existing == null) {
            if (platformCredentialCount > 0) {
                throw bootstrapClosed();
            }
            /** 待新增的首位平台凭证。 */
            CredentialEntity credential = new CredentialEntity();
            credential.setSubjectType(PLATFORM_IDENTITY);
            credential.setSubjectId(platformIdentityId);
            credential.setPasswordHash(encodedPassword);
            credential.setAlgorithm(AuthSecurityConfiguration.ARGON2ID_ALGORITHM);
            credential.setPasswordChangedAt(OffsetDateTime.now());
            credential.setCredentialVersion(1L);
            credential.setStatus("ACTIVE");
            credentialMapper.insertCredential(credential);
        } else if (!AuthSecurityConfiguration.ARGON2ID_ALGORITHM.equals(existing.getAlgorithm())
                || !passwordEncoder.matches(rawPassword, existing.getPasswordHash())) {
            throw bootstrapClosed();
        }
        return replaceRecoveryCodes(platformIdentityId, recoveryCodeCount);
    }

    /**
     * 废弃旧批次并生成新的恢复码批次。
     *
     * @param platformIdentityId 平台身份 ID
     * @param recoveryCodeCount 恢复码数量
     * @return 恢复码明文，仅当前事务调用链持有
     */
    private List<String> replaceRecoveryCodes(long platformIdentityId, int recoveryCodeCount) {
        recoveryCodeMapper.supersedeActiveCodes(platformIdentityId);
        /** 新恢复码批次 ID。 */
        String batchId = UUID.randomUUID().toString();
        /** 仅本次响应使用的恢复码明文集合。 */
        List<String> recoveryCodes = new ArrayList<>(recoveryCodeCount);
        for (int index = 0; index < recoveryCodeCount; index++) {
            /** 新生成的高熵恢复码。 */
            String recoveryCode = recoveryCodeGenerator.generate();
            /** 只持久化摘要的恢复码实体。 */
            PlatformRecoveryCodeEntity entity = new PlatformRecoveryCodeEntity();
            entity.setPlatformIdentityId(platformIdentityId);
            entity.setBatchId(batchId);
            entity.setCodeHash(recoveryCodeGenerator.hash(recoveryCode));
            entity.setStatus("ACTIVE");
            recoveryCodeMapper.insertRecoveryCode(entity);
            recoveryCodes.add(recoveryCode);
        }
        return List.copyOf(recoveryCodes);
    }

    /** @return 统一引导关闭业务异常 */
    private BusinessException bootstrapClosed() {
        return new BusinessException(BOOTSTRAP_CLOSED_CODE, "首位平台管理员引导已经关闭", 409);
    }
}
