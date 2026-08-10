package com.canteen.smile.modules.permission.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.permission.dto.CreatePermissionResourceRequest;
import com.canteen.smile.modules.permission.dto.PermissionResourcePageQuery;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import com.canteen.smile.modules.permission.service.PermissionResourceService;
import com.canteen.smile.modules.permission.vo.PermissionResourceVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 平台权限资源发布与永久废弃接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.PLATFORM_PERMISSION_RESOURCES)
public class PlatformPermissionResourceController {

    /** 权限资源领域服务。 */
    private final PermissionResourceService service;

    /** @param query 分页条件 @return 权限资源分页 */
    @GetMapping
    @SaCheckPermission(IamPermissionCodes.PLATFORM_PERMISSION_MANAGE)
    public ApiResponse<PageResult<PermissionResourceVO>> page(
            @Valid @ModelAttribute PermissionResourcePageQuery query
    ) {
        return ApiResponse.success(service.page(query));
    }

    /** @param request 草稿参数 @return 新建草稿 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission(IamPermissionCodes.PLATFORM_PERMISSION_MANAGE)
    public ApiResponse<PermissionResourceVO> create(
            @Valid @RequestBody CreatePermissionResourceRequest request
    ) {
        return ApiResponse.success(service.create(request));
    }

    /** @param resourceId 资源 ID @param version 乐观锁版本 @return 已发布资源 */
    @PostMapping("/{resourceId}/actions/publish")
    @SaCheckPermission(IamPermissionCodes.PLATFORM_PERMISSION_MANAGE)
    public ApiResponse<PermissionResourceVO> publish(
            @Positive @PathVariable long resourceId,
            @PositiveOrZero @RequestParam long version
    ) {
        return ApiResponse.success(service.publish(resourceId, version));
    }

    /** @param resourceId 资源 ID @param version 乐观锁版本 @return 已废弃资源 */
    @PostMapping("/{resourceId}/actions/deprecate")
    @SaCheckPermission(IamPermissionCodes.PLATFORM_PERMISSION_MANAGE)
    public ApiResponse<PermissionResourceVO> deprecate(
            @Positive @PathVariable long resourceId,
            @PositiveOrZero @RequestParam long version
    ) {
        return ApiResponse.success(service.deprecate(resourceId, version));
    }
}
