package com.canteen.smile;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/** 认证与授权微服务启动入口。 */
@EnableAsync
@MapperScan(basePackages = "com.canteen.smile.modules", annotationClass = Mapper.class)
@SpringBootApplication
public class AuthServiceApplication {

    /**
     * 启动认证与授权微服务。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
