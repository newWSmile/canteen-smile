package com.canteen.smile.internal.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.internal.dto.AuthAuditLogInternalResponse;
import com.canteen.smile.internal.dto.AuthAuditLogSearchRequest;
import com.canteen.smile.modules.audit.service.AuthAuditLogQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 仅供 IAM 经内部网络与 HMAC 查询 Auth 审计的接口。 */
@RestController
@RequiredArgsConstructor
public class AuthAuditLogInternalController {

    /** Auth 审计只读查询服务。 */
    private final AuthAuditLogQueryService service;

    /** @param request 已签名分页条件 @return Auth 自有审计分页结果 */
    @PostMapping(AuthApiPaths.INTERNAL_AUDIT_LOG_SEARCH)
    public ApiResponse<PageResult<AuthAuditLogInternalResponse>> page(
            @Valid @RequestBody AuthAuditLogSearchRequest request
    ) {
        return ApiResponse.success(service.page(request));
    }
}
