package com.canteen.smile.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 手机号可轮换加密密钥配置，真实密钥只能由环境变量或密钥服务注入。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.mobile-encryption")
public class MobileEncryptionProperties {

    /** 当前加密密钥版本标识，用于后续密钥轮换和密文解密路由。 */
    private String keyId;

    /** Base64 编码的 256 位 AES 密钥，禁止写入代码仓库。 */
    private String key;
}
