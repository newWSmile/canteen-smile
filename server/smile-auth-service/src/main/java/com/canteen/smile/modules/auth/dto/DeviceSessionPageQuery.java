package com.canteen.smile.modules.auth.dto;

import com.canteen.smile.common.api.PageConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/** 当前账号设备会话分页查询参数。 */
@Getter
@Setter
public class DeviceSessionPageQuery {

    /** 页码，从 1 开始。 */
    @Min(1)
    private int pageNo = PageConstants.DEFAULT_PAGE_NO;

    /** 每页数量，最大 100。 */
    @Min(1)
    @Max(100)
    private int pageSize = PageConstants.DEFAULT_PAGE_SIZE;
}
