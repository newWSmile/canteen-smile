package com.canteen.smile.modules.audit.dto;

import com.canteen.smile.common.api.PageConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

/** 平台端和租户端统一审计日志分页查询参数。 */
@Getter
@Setter
public class AuditLogPageQuery {

    /** 当前页码，从 1 开始。 */
    @Min(value = PageConstants.DEFAULT_PAGE_NO, message = "pageNo 不能小于 1")
    private int pageNo = PageConstants.DEFAULT_PAGE_NO;

    /** 每页数量，最大 100。 */
    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = PageConstants.MAX_PAGE_SIZE, message = "pageSize 不能大于 100")
    private int pageSize = PageConstants.DEFAULT_PAGE_SIZE;

    /** 审计数据来源：IAM 管理审计或 Auth 认证安全审计。 */
    @Pattern(regexp = "IAM|AUTH", message = "source 只能是 IAM 或 AUTH")
    private String source = "IAM";

    /** 可选精确动作编码。 */
    @Size(max = 128, message = "actionCode 长度不能超过 128")
    @Pattern(regexp = "[A-Za-z0-9:_-]+", message = "actionCode 格式不正确")
    private String actionCode;

    /** 可选操作结果。 */
    @Pattern(regexp = "SUCCESS|FAILURE|DENIED", message = "result 不是有效的审计结果")
    private String result;

    /** 可选精确操作者 ID。 */
    @PositiveOrZero(message = "operatorId 不能为负数")
    private Long operatorId;

    /** 可选查询开始时间。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startTime;

    /** 可选查询结束时间。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime endTime;
}
