package com.canteen.smile.modules.sms.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.canteen.smile.modules.sms.model.SmsPurpose;

import java.time.OffsetDateTime;

/**
 * 本地开发短信策略。
 *
 * <p>该策略不连接真实运营商，只记录脱敏发送元数据。完整手机号、验证码和模板参数
 * 不会写入普通日志；数据库投递记录由统一发送服务持久化。</p>
 */
@Component
@Profile({"local", "dev", "test"})
public class LocalDatabaseLogSmsClient implements SmsClient {

    /** 本地策略固定编码。 */
    public static final String PROVIDER_CODE = "LOCAL_DATABASE_LOG";

    /** 当前策略日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(LocalDatabaseLogSmsClient.class);

    /** {@inheritDoc} */
    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    /** 本地策略不读取或初始化真实供应商和模板配置。 */
    @Override
    public SmsClientConfiguration configurationFor(SmsPurpose purpose) {
        return SmsClientConfiguration.localUnmanaged();
    }

    /**
     * 在本地环境接受发送请求并输出脱敏元数据。
     *
     * @param command 发送命令
     * @return 本地接受结果
     */
    @Override
    public SmsSendResult send(SmsSendCommand command) {
        OffsetDateTime acceptedTime = OffsetDateTime.now();
        log.debug(
                "Local SMS strategy accepted request: requestId={}, challengeId={}, purpose={}, templateCode={}",
                command.requestId(),
                command.challengeId(),
                command.purpose(),
                command.templateCode()
        );
        return new SmsSendResult(command.requestId(), acceptedTime);
    }
}
