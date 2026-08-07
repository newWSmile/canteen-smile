package com.canteen.smile.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigDecimal;

/** API 网关启动入口。 */
@SpringBootApplication
public class GatewayApplication {

    /**
     * 启动 API 网关。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
