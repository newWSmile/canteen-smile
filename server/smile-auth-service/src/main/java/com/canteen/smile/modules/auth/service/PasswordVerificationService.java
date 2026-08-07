package com.canteen.smile.modules.auth.service;

import com.canteen.smile.config.AuthSecurityConfiguration;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.model.AuthConstants;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** 使用 Argon2id 恒定执行一次密码验证，降低用户名存在性侧信道。 */
@Service
public class PasswordVerificationService {

    /** Argon2id 密码编码器。 */
    private final PasswordEncoder passwordEncoder;

    /** 启动时生成且只用于不存在账号校验的随机摘要。 */
    private final String dummyPasswordHash;

    /** @param passwordEncoder Argon2id 密码编码器 */
    public PasswordVerificationService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * 校验有效凭证密码；无效主体仍执行相同 Argon2id 比较。
     *
     * @param rawPassword 请求密码
     * @param credential 查询到的凭证，可为空
     * @return 是否为有效凭证且密码匹配
     */
    public boolean matches(String rawPassword, CredentialEntity credential) {
        /** 可执行比较的真实或占位摘要。 */
        String encodedPassword = usable(credential) ? credential.getPasswordHash() : dummyPasswordHash;
        boolean matched;
        try {
            matched = passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException exception) {
            matched = false;
        }
        return usable(credential) && matched;
    }

    /** @param credential 凭证实体 @return 是否为当前支持的有效 Argon2id 凭证 */
    private boolean usable(CredentialEntity credential) {
        return credential != null
                && AuthConstants.ACTIVE_STATUS.equals(credential.getStatus())
                && AuthSecurityConfiguration.ARGON2ID_ALGORITHM.equals(credential.getAlgorithm())
                && credential.getPasswordHash() != null;
    }
}
