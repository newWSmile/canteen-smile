package com.canteen.smile.internal.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** IAM 调用 Auth 的 HMAC v1 客户端配置。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.auth-client-hmac")
public class AuthHmacClientProperties {

    /** IAM 调用方服务标识。 */
    private String callerId = "smile-iam-service";

    /** 当前 HMAC 密钥版本。 */
    private String keyId = "v1";

    /** IAM 与 Auth 共享的 HMAC 密钥。 */
    private String secret;
}
