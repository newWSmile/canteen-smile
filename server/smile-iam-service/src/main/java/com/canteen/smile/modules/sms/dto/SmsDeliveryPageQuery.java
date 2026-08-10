package com.canteen.smile.modules.sms.dto;

import com.canteen.smile.common.api.PageConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

/** 平台短信发送记录分页查询参数。 */
@Getter
@Setter
public class SmsDeliveryPageQuery {

    /** 当前页码，从 1 开始。 */
    @Min(value = PageConstants.DEFAULT_PAGE_NO, message = "pageNo 不能小于 1")
    private int pageNo = PageConstants.DEFAULT_PAGE_NO;

    /** 每页数量，最大 100。 */
    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = PageConstants.MAX_PAGE_SIZE, message = "pageSize 不能大于 100")
    private int pageSize = PageConstants.DEFAULT_PAGE_SIZE;

    /** 可选完整手机号，只用于服务端 HMAC 精确查询。 */
    @Size(max = 32, message = "手机号长度不能超过 32")
    private String mobile;

    /** 可选查询开始时间。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startTime;

    /** 可选查询结束时间。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime endTime;
}
