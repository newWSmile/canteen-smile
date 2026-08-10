package com.canteen.smile.modules.account.dto;

import com.canteen.smile.common.api.PageConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 本机构用户分页查询条件。 */
@Getter
@Setter
public class TenantUserPageQuery {
    /** 页码。 */
    @Min(1) private int pageNo = PageConstants.DEFAULT_PAGE_NO;
    /** 每页数量。 */
    @Min(1) @Max(100) private int pageSize = PageConstants.DEFAULT_PAGE_SIZE;
    /** 用户名前缀或显示名称关键词。 */
    @Size(max = 128) private String keyword;
    /** 可选账号状态。 */
    @Pattern(regexp = "PENDING_ACTIVATION|ACTIVE|PASSWORD_RESET_REQUIRED|DISABLED|CANCELLED")
    private String status;
}
