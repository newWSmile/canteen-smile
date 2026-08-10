package com.canteen.smile.internal.dto;

/** Auth 对 IAM 安全事件的幂等消费结果。 */
public record SecurityEventResponse(String eventId, String result, boolean alreadyConsumed) {
}
