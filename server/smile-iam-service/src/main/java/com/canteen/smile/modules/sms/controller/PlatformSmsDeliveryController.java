package com.canteen.smile.modules.sms.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import com.canteen.smile.modules.sms.dto.SmsDeliveryPageQuery;
import com.canteen.smile.modules.sms.service.PlatformSmsDeliveryService;
import com.canteen.smile.modules.sms.vo.SmsDeliveryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 平台超级管理员短信发送记录查询接口。 */
@RestController
@RequiredArgsConstructor
public class PlatformSmsDeliveryController {

    /** 平台短信记录查询服务。 */
    private final PlatformSmsDeliveryService service;

    /** @param query 分页筛选条件 @return 短信发送记录分页 */
    @PostMapping(IamApiPaths.PLATFORM_SMS_DELIVERY_SEARCH)
    @SaCheckPermission(IamPermissionCodes.PLATFORM_SMS_DELIVERY_VIEW)
    public ApiResponse<PageResult<SmsDeliveryVO>> page(@Valid @RequestBody SmsDeliveryPageQuery query) {
        return ApiResponse.success(service.page(query));
    }
}
