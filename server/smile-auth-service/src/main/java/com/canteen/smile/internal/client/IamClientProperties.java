package com.canteen.smile.internal.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** IAM 静态地址和超时配置；接入 Nacos 后保持 Client 契约不变。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.clients.iam")
public class IamClientProperties {

    /** IAM 服务基础地址。 */
    private String baseUrl = "http://127.0.0.1:8082";

    /** 连接超时毫秒数。 */
    private int connectTimeoutMillis = 2_000;

    /** 读取超时毫秒数。 */
    private int readTimeoutMillis = 3_000;
}
