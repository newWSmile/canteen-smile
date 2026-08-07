package com.canteen.smile.internal.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Auth 调 IAM 的 HMAC v1 客户端配置。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.internal-hmac")
public class InternalHmacClientProperties {

    /** Auth 调用方服务标识。 */
    private String callerId = "smile-auth-service";

    /** 当前 HMAC 密钥版本。 */
    private String keyId = "v1";

    /** Auth → IAM HMAC 密钥。 */
    private String secret;
}
