package com.canteen.smile.internal.client.dto;

import java.time.OffsetDateTime;

/**
 * Auth 返回 IAM 的短信投递审计投影；正文是否含验证码由记录创建时的安全策略决定。
 *
 * @param id 记录 ID
 * @param requestId 请求唯一标识
 * @param challengeId 关联挑战标识
 * @param providerCode 策略编码
 * @param purpose 业务用途
 * @param maskedMobile 脱敏手机号
 * @param templateCode 模板编码
 * @param content 短信正文快照，可能按显式安全策略保留验证码明文
 * @param sensitiveContentRetained 正文是否按安全策略保留验证码等敏感内容
 * @param status 投递状态
 * @param providerMessageId 供应商消息标识
 * @param failureCode 失败编码
 * @param failureMessage 脱敏失败说明
 * @param acceptedTime 策略接受时间
 * @param createdTime 创建时间
 */
public record SmsDeliveryInternalResponse(
        String id,
        String requestId,
        String challengeId,
        String providerCode,
        String purpose,
        String maskedMobile,
        String templateCode,
        String content,
        boolean sensitiveContentRetained,
        String status,
        String providerMessageId,
        String failureCode,
        String failureMessage,
        OffsetDateTime acceptedTime,
        OffsetDateTime createdTime
) {
}
