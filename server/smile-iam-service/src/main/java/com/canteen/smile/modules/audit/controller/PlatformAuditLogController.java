package com.canteen.smile.modules.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.audit.dto.AuditLogPageQuery;
import com.canteen.smile.modules.audit.service.AuditLogQueryService;
import com.canteen.smile.modules.audit.vo.AuditLogVO;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 平台超级管理员审计日志查询接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.PLATFORM_AUDIT_LOGS)
public class PlatformAuditLogController {

    /** 统一审计查询服务。 */
    private final AuditLogQueryService service;

    /** @param query 分页筛选条件 @return 平台审计分页 */
    @GetMapping
    @SaCheckPermission(IamPermissionCodes.PLATFORM_AUDIT_VIEW)
    public ApiResponse<PageResult<AuditLogVO>> page(@Valid @ModelAttribute AuditLogPageQuery query) {
        return ApiResponse.success(service.pagePlatform(query));
    }
}
