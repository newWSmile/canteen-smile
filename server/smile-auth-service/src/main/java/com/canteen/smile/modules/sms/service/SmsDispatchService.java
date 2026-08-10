package com.canteen.smile.modules.sms.service;

import com.canteen.smile.config.SmsProperties;
import com.canteen.smile.modules.sms.client.SmsClient;
import com.canteen.smile.modules.sms.client.SmsSendCommand;
import com.canteen.smile.modules.sms.client.SmsSendResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import com.canteen.smile.modules.sms.model.SmsRuntimePolicy;

/** 统一短信发送编排：选择策略、原子防重、发送并持久化脱敏结果。 */
@Service
@RequiredArgsConstructor
public class SmsDispatchService {

    /** 当前服务日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(SmsDispatchService.class);

    /** 短信策略选择配置。 */
    private final SmsProperties smsProperties;

    /** 短信策略路由。 */
    private final SmsClientRouter smsClientRouter;

    /** 短信正文敏感值脱敏器。 */
    private final SmsContentSanitizer smsContentSanitizer;

    /** 手机号摘要和脱敏服务。 */
    private final MobileProtectionService mobileProtectionService;

    /** 脱敏投递记录服务。 */
    private final SmsDeliveryRecordService smsDeliveryRecordService;

    /** 当前数据库短信安全策略。 */
    private final SmsRuntimePolicyService smsRuntimePolicyService;

    /**
     * 发送短信；完整手机号和模板参数只在当前调用栈内传递。
     *
     * @param command 发送命令
     * @return 策略接受结果
     */
    public SmsSendResult dispatch(SmsSendCommand command) {
        validate(command);
        MobileProtectionService.ProtectedMobile protectedMobile = mobileProtectionService.protect(command.mobile());
        SmsRuntimePolicy policy = smsRuntimePolicyService.current();
        String contentSnapshot = smsContentSanitizer.sanitize(
                command.renderedContent(),
                command.sensitiveValues(),
                command.mobile(),
                protectedMobile.masked(),
                policy.plaintextCodeRetentionEnabled()
        );
        String logContent = smsContentSanitizer.sanitize(
                command.renderedContent(), command.sensitiveValues(), command.mobile(),
                protectedMobile.masked(), false
        );
        SmsClient client = smsClientRouter.resolve(smsProperties.getProviderCode());
        String providerCode = client.providerCode();
        smsDeliveryRecordService.claim(
                providerCode, command, contentSnapshot, protectedMobile,
                policy.plaintextCodeRetentionEnabled()
        );

        SmsSendResult result;
        try {
            result = client.send(command);
        } catch (RuntimeException exception) {
            smsDeliveryRecordService.recordFailed(
                    command.requestId(),
                    SmsDispatchException.DELIVERY_FAILED,
                    "短信策略处理失败"
            );
            log.warn(
                    "SMS delivery failed: requestId={}, providerCode={}, purpose={}, mobile={}, cause={}",
                    command.requestId(),
                    providerCode,
                    command.purpose(),
                    protectedMobile.masked(),
                    exception.getClass().getSimpleName()
            );
            throw new SmsDispatchException(
                    SmsDispatchException.DELIVERY_FAILED,
                    "短信发送暂时不可用",
                    exception
            );
        }

        smsDeliveryRecordService.recordAccepted(command.requestId(), result);
        log.info(
                "SMS delivery accepted: requestId={}, providerCode={}, purpose={}, mobile={}, content={}",
                command.requestId(),
                providerCode,
                command.purpose(),
                protectedMobile.masked(),
                logContent
        );
        return result;
    }

    /** 校验策略执行和数据库约束需要的发送命令字段。 */
    private void validate(SmsSendCommand command) {
        if (command == null
                || isBlank(command.requestId())
                || command.purpose() == null
                || isBlank(command.mobile())) {
            throw new IllegalArgumentException("SMS send command is incomplete");
        }
        Map<String, String> parameters = command.templateParameters();
        if (parameters == null) {
            throw new IllegalArgumentException("SMS template parameters must not be null");
        }
        if (command.sensitiveValues() == null) {
            throw new IllegalArgumentException("SMS sensitive values must not be null");
        }
    }

    /** 判断文本是否为空。 */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
