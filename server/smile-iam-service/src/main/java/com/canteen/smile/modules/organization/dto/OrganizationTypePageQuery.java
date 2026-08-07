package com.canteen.smile.modules.organization.dto;

import com.canteen.smile.common.api.PageConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** 租户机构类型分页查询参数。 */
@Getter
@Setter
public class OrganizationTypePageQuery {

    /** 当前页码。 */
    @Min(1)
    private int pageNo = PageConstants.DEFAULT_PAGE_NO;

    /** 每页数量，最大 100。 */
    @Min(1)
    @Max(100)
    private int pageSize = PageConstants.DEFAULT_PAGE_SIZE;

    /** 可选机构类型状态。 */
    @Pattern(regexp = "ACTIVE|DISABLED")
    private String status;
}
