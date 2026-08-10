package com.canteen.smile.internal.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.infrastructure.security.InternalHmacHeaders;
import com.canteen.smile.internal.dto.SecurityEventRequest;
import com.canteen.smile.internal.dto.SecurityEventResponse;
import com.canteen.smile.modules.securityevent.service.SecurityEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** 仅供 IAM 经内部网络和 HMAC 调用的安全事件消费接口。 */
@RestController
@RequiredArgsConstructor
public class SecurityEventInternalController {

    /** 安全事件幂等消费服务。 */
    private final SecurityEventService service;

    /** 消费一条 IAM Outbox 安全事件。 */
    @PostMapping(AuthApiPaths.INTERNAL_SECURITY_EVENTS)
    public ApiResponse<SecurityEventResponse> consume(
            @RequestHeader(InternalHmacHeaders.EVENT_ID) String signedEventId,
            @Valid @RequestBody SecurityEventRequest request
    ) {
        return ApiResponse.success(service.consume(signedEventId, request));
    }
}
