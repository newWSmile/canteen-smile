package com.canteen.smile.internal.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.internal.dto.ConsumeReauthTicketRequest;
import com.canteen.smile.modules.auth.service.InternalReauthTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 仅供 IAM 经 HMAC 调用的再认证票据消费接口。 */
@RestController
@RequiredArgsConstructor
public class ReauthTicketInternalController {

    /** 再认证票据消费服务。 */
    private final InternalReauthTicketService service;

    /** @param request 待消费票据及主体上下文 @return 空成功响应 */
    @PostMapping(AuthApiPaths.INTERNAL_REAUTH_TICKET_CONSUME)
    public ApiResponse<Void> consume(@Valid @RequestBody ConsumeReauthTicketRequest request) {
        service.consume(request.ticket(), request.subjectType(), Long.parseLong(request.subjectId()),
                request.allowedAction());
        return ApiResponse.success(null);
    }
}
