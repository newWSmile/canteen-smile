package com.canteen.smile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Auth 密码哈希安全配置。 */
@Configuration
public class AuthSecurityConfiguration {

    /** Argon2id 算法标识及参数版本。 */
    public static final String ARGON2ID_ALGORITHM = "ARGON2ID_V1";

    /**
     * 创建 Argon2id 密码编码器。
     * 当前 v1 参数为 16 字节盐、32 字节摘要、单并行度、64 MiB 内存和 3 次迭代；
     * 参数升级必须发布新算法版本并在成功登录时渐进迁移。
     *
     * @return Argon2id 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65_536, 3);
    }
}
