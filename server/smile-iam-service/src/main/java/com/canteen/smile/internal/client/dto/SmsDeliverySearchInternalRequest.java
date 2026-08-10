package com.canteen.smile.internal.client.dto;

import java.time.OffsetDateTime;

/**
 * IAM 传给 Auth 的短信投递分页条件。
 *
 * @param pageNo 页码
 * @param pageSize 页大小
 * @param mobile 可选完整手机号，仅供 Auth 计算查询摘要
 * @param startTime 可选开始时间
 * @param endTime 可选结束时间
 */
public record SmsDeliverySearchInternalRequest(
        int pageNo,
        int pageSize,
        String mobile,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}
