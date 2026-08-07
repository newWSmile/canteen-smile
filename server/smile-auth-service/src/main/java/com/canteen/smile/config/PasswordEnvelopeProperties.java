package com.canteen.smile.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 密码信封加密挑战的运行参数。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.password-envelope")
public class PasswordEnvelopeProperties {

    /** 一次性密码加密挑战的有效秒数。 */
    private int challengeTtlSeconds = 120;

    /** 临时 RSA 密钥的位数，仅用于包装一次性 AES-256 密钥。 */
    private int rsaKeySize = 2048;

    /** RSA 密钥对轮换周期天数；挑战本身仍按秒级有效期一次性消费。 */
    private int rsaKeyRotationDays = 30;
}
