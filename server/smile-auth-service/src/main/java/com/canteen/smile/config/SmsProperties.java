package com.canteen.smile.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 短信发送策略选择配置；真实供应商密钥不得放入该普通配置对象。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.sms")
public class SmsProperties {

    /** 当前启用的短信策略编码，本地开发默认使用数据库加脱敏日志策略。 */
    private String providerCode = "LOCAL_DATABASE_LOG";

    /** 计算手机号查询 HMAC 的服务端 Pepper，只能通过环境变量或密钥服务注入。 */
    private String mobileHashPepper;

    /** 计算验证码 HMAC 的服务端 Pepper；默认可由手机号 Pepper 通过配置显式回退。 */
    private String codeHashPepper;

    /** 验证码有效秒数，默认五分钟。 */
    private int challengeTtlSeconds = 300;

    /** 同一手机号两次发送之间的最小秒数。 */
    private int resendIntervalSeconds = 60;

    /** 单个验证码允许的最大错误次数，数据库约束固定不超过五次。 */
    private int maxVerificationAttempts = 5;

    /** 同一手机号每小时最多发送次数。 */
    private int mobileHourlyLimit = 5;

    /** 同一手机号每日最多发送次数。 */
    private int mobileDailyLimit = 10;

    /** 同一来源 IP 每小时最多发送次数。 */
    private int ipHourlyLimit = 30;

    /** 同一来源 IP 每日最多发送次数。 */
    private int ipDailyLimit = 100;

    /** 同一设备每小时最多发送次数。 */
    private int deviceHourlyLimit = 10;

    /** 同一设备每日最多发送次数。 */
    private int deviceDailyLimit = 30;
}
