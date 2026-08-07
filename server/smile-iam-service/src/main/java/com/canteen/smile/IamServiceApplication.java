package com.canteen.smile;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/** 租户身份与访问管理微服务启动入口。 */
@EnableAsync
@MapperScan("com.canteen.smile.modules")
@SpringBootApplication
public class IamServiceApplication {

    /**
     * 启动身份与访问管理微服务。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(IamServiceApplication.class, args);
    }
}
