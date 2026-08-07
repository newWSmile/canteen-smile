package com.canteen.smile.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 首位平台超级管理员一次性引导配置。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.bootstrap")
public class BootstrapProperties {

    /** 环境变量提供的一次性高熵引导密钥。 */
    private String secret;

    /** 首次生成并只展示一次的恢复码数量。 */
    private int recoveryCodeCount = 10;
}
