package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.mapper.PasswordHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/** 校验当前密码及最近五次密码不可重复。 */
@Service
@RequiredArgsConstructor
public class PasswordHistoryService {

    /** 需要禁止复用的历史密码数量。 */
    private static final int HISTORY_LIMIT = 5;

    /** 密码复用错误码。 */
    private static final String PASSWORD_REUSED_CODE = "AUTH_1207";

    /** 密码历史数据访问接口。 */
    private final PasswordHistoryMapper passwordHistoryMapper;

    /** Argon2id 密码编码器。 */
    private final PasswordEncoder passwordEncoder;

    /**
     * 校验新密码没有与当前密码或最近历史密码重复。
     *
     * @param rawPassword 新密码明文，仅在当前调用栈短暂存在
     * @param credential 当前凭证
     */
    public void validateNotReused(String rawPassword, CredentialEntity credential) {
        if (credential.getPasswordHash() != null
                && passwordEncoder.matches(rawPassword, credential.getPasswordHash())) {
            throw reused();
        }
        /** 最近五次历史密码摘要。 */
        List<String> recentHashes = passwordHistoryMapper.selectRecentHashes(
                credential.getSubjectType(),
                credential.getSubjectId(),
                HISTORY_LIMIT
        );
        for (String passwordHash : recentHashes) {
            if (passwordEncoder.matches(rawPassword, passwordHash)) {
                throw reused();
            }
        }
    }

    /** @return 统一密码复用异常 */
    private BusinessException reused() {
        return new BusinessException(PASSWORD_REUSED_CODE, "新密码不能与当前密码或最近五次密码相同");
    }
}
