package com.canteen.smile.internal.client.dto;

/** Auth 安全事件幂等消费结果。 */
public record SecurityEventInternalResponse(String eventId, String result, boolean alreadyConsumed) {
}
