package com.canteen.smile.modules.sms.service;

import com.canteen.smile.modules.sms.client.SmsSendCommand;
import com.canteen.smile.modules.sms.client.SmsSendResult;
import com.canteen.smile.modules.sms.mapper.SmsDeliveryRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 短信发送请求的原子领取和脱敏投递状态记录服务。 */
@Service
@RequiredArgsConstructor
public class SmsDeliveryRecordService {

    /** 短信投递记录 Mapper。 */
    private final SmsDeliveryRecordMapper smsDeliveryRecordMapper;

    /**
     * 原子领取请求 ID，阻止相同请求被重复发送。
     *
     * @param providerCode 实际短信策略编码
     * @param command 发送命令
     * @param contentSnapshot 已脱敏短信正文快照
     * @param protectedMobile 手机号安全投影
     * @param sensitiveContentRetained 正文是否保留验证码等敏感内容
     */
    @Transactional
    public void claim(
            String providerCode,
            SmsSendCommand command,
            String contentSnapshot,
            MobileProtectionService.ProtectedMobile protectedMobile,
            boolean sensitiveContentRetained
    ) {
        int inserted = smsDeliveryRecordMapper.insertProcessing(
                command.requestId(),
                command.challengeId(),
                providerCode,
                command.purpose().name(),
                protectedMobile.masked(),
                protectedMobile.hash(),
                command.templateCode(),
                contentSnapshot,
                sensitiveContentRetained
        );
        if (inserted != 1) {
            throw new SmsDispatchException(
                    SmsDispatchException.DUPLICATE_REQUEST,
                    "短信发送请求已处理，请勿重复提交"
            );
        }
    }

    /**
     * 记录短信策略已接受请求。
     *
     * @param requestId 发送请求唯一标识
     * @param result 策略接受结果
     */
    @Transactional
    public void recordAccepted(String requestId, SmsSendResult result) {
        int updated = smsDeliveryRecordMapper.markAccepted(
                requestId,
                result.providerMessageId(),
                result.acceptedTime()
        );
        requireSingleUpdate(updated, requestId);
    }

    /**
     * 记录短信策略执行失败。
     *
     * @param requestId 发送请求唯一标识
     * @param failureCode 稳定内部失败编码
     * @param failureMessage 脱敏失败说明
     */
    @Transactional
    public void recordFailed(String requestId, String failureCode, String failureMessage) {
        int updated = smsDeliveryRecordMapper.markFailed(requestId, failureCode, failureMessage);
        requireSingleUpdate(updated, requestId);
    }

    /** 确保状态迁移只影响已经领取的一条发送请求。 */
    private void requireSingleUpdate(int updated, String requestId) {
        if (updated != 1) {
            throw new IllegalStateException("SMS delivery state transition failed for request: " + requestId);
        }
    }
}
