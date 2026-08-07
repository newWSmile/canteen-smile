package com.canteen.smile.modules.organization.dto;

import com.canteen.smile.common.api.PageConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** 机构直属子节点分页查询参数。 */
@Getter
@Setter
public class OrganizationPageQuery {

    /** 父机构 ID。 */
    @Positive
    private long parentId;

    /** 当前页码。 */
    @Min(1)
    private int pageNo = PageConstants.DEFAULT_PAGE_NO;

    /** 每页数量。 */
    @Min(1)
    @Max(100)
    private int pageSize = PageConstants.DEFAULT_PAGE_SIZE;
}
