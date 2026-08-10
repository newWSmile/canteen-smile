package com.canteen.smile.internal.client.dto;

/** IAM 发给 Auth 的短信敏感正文留存策略更新契约。 */
public record SmsSecurityPolicyUpdateInternalRequest(
        boolean plaintextCodeRetentionEnabled,
        long version,
        long actorId,
        String reauthTicket
) {
}
