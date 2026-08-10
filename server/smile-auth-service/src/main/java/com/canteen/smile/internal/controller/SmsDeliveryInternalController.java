package com.canteen.smile.internal.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.internal.dto.SmsDeliveryInternalResponse;
import com.canteen.smile.internal.dto.SmsDeliverySearchRequest;
import com.canteen.smile.modules.sms.service.SmsDeliveryQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 仅供 IAM 经内部网络和 HMAC 查询短信投递记录的接口。 */
@RestController
@RequiredArgsConstructor
public class SmsDeliveryInternalController {

    /** 短信投递查询服务。 */
    private final SmsDeliveryQueryService service;

    /** @param request 已签名分页条件 @return 短信投递分页 */
    @PostMapping(AuthApiPaths.INTERNAL_SMS_DELIVERY_SEARCH)
    public ApiResponse<PageResult<SmsDeliveryInternalResponse>> page(
            @Valid @RequestBody SmsDeliverySearchRequest request
    ) {
        return ApiResponse.success(service.page(request));
    }
}
