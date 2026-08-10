package com.canteen.smile.modules.permission.dto;

import com.canteen.smile.common.api.PageConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** 平台权限资源分页查询参数。 */
@Getter
@Setter
public class PermissionResourcePageQuery {

    /** 当前页码。 */
    @Min(1)
    private int pageNo = PageConstants.DEFAULT_PAGE_NO;
    /** 每页数量，最大 100。 */
    @Min(1)
    @Max(100)
    private int pageSize = PageConstants.DEFAULT_PAGE_SIZE;
    /** 可选发布状态。 */
    @Pattern(regexp = "DRAFT|PUBLISHED|DEPRECATED")
    private String publishStatus;
    /** 可选应用编码。 */
    @Pattern(regexp = "PLATFORM_ADMIN|TENANT_ADMIN|TENANT_PORTAL|SERVICE")
    private String appCode;
    /** 可选资源类型。 */
    @Pattern(regexp = "DIRECTORY|MENU|BUTTON|API|DATA_MODULE")
    private String resourceType;
}
