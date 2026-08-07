package com.canteen.smile.internal.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Auth 调用 IAM 内部接口的 HMAC 校验配置。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.internal-hmac")
public class IamInternalHmacProperties {

    /** 唯一允许调用当前 IAM 内部接口的服务标识。 */
    private String allowedCallerId = "smile-auth-service";

    /** 当前生效的密钥版本标识。 */
    private String keyId = "v1";

    /** HMAC 密钥，只允许来自环境变量或密钥服务。 */
    private String secret;

    /** 允许的请求时钟偏差秒数。 */
    private long allowedClockSkewSeconds = 300;

    /** 已使用 Nonce 的 Redis 保留秒数。 */
    private long nonceTtlSeconds = 600;
}
