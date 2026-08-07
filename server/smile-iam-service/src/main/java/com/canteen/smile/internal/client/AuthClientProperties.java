package com.canteen.smile.internal.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Auth 静态地址和超时配置；接入服务发现后保持 Client 契约不变。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.clients.auth")
public class AuthClientProperties {

    /** Auth 服务基础地址。 */
    private String baseUrl = "http://127.0.0.1:8081";

    /** 连接超时毫秒数。 */
    private int connectTimeoutMillis = 2_000;

    /** 读取超时毫秒数。 */
    private int readTimeoutMillis = 3_000;
}
