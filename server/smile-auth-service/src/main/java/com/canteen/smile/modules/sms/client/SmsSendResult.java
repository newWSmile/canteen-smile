package com.canteen.smile.modules.sms.client;

import java.time.OffsetDateTime;

/**
 * 短信策略接受结果。
 *
 * @param providerMessageId 供应商消息标识，本地策略使用请求标识
 * @param acceptedTime 策略接受请求的时间
 */
public record SmsSendResult(String providerMessageId, OffsetDateTime acceptedTime) {
}
