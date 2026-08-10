package com.canteen.smile.internal.client.dto;

/**
 * IAM 请求 Auth 消费再认证票据的 v1 契约。
 *
 * @param ticket 一次性票据
 * @param subjectType 主体类型
 * @param subjectId 主体 ID
 * @param allowedAction 唯一允许动作
 */
public record ConsumeReauthTicketInternalRequest(
        String ticket,
        String subjectType,
        String subjectId,
        String allowedAction
) {
}
