package com.canteen.smile.internal.dto;

import com.canteen.smile.common.api.PageConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * IAM 经 HMAC 传入的短信投递分页条件。
 *
 * @param pageNo 页码
 * @param pageSize 页大小
 * @param mobile 可选完整手机号，仅在 Auth 内存中转换为查询摘要
 * @param startTime 可选开始时间
 * @param endTime 可选结束时间
 */
public record SmsDeliverySearchRequest(
        @Min(1) int pageNo,
        @Min(1) @Max(PageConstants.MAX_PAGE_SIZE) int pageSize,
        @Size(max = 32) String mobile,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}
