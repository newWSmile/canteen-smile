package com.canteen.smile.modules.tenant.dto;

import com.canteen.smile.common.api.PageConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** 平台端租户分页查询参数。 */
@Getter
@Setter
public class TenantPageQuery {

    /** 当前页码，从 1 开始。 */
    @Min(value = PageConstants.DEFAULT_PAGE_NO, message = "pageNo 不能小于 1")
    private int pageNo = PageConstants.DEFAULT_PAGE_NO;

    /** 每页数量，最大 100。 */
    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = PageConstants.MAX_PAGE_SIZE, message = "pageSize 不能大于 100")
    private int pageSize = PageConstants.DEFAULT_PAGE_SIZE;

    /** 可选租户生命周期状态。 */
    @Pattern(
            regexp = "INITIALIZING|ACTIVE|SUSPENDED|EXPIRED|CANCELLED",
            message = "status 不是有效的租户状态"
    )
    private String status;

}
