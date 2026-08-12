package com.canteen.smile.modules.auth.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.auth.dto.DeviceSessionPageQuery;
import com.canteen.smile.modules.auth.dto.LogoutDeviceSessionRequest;
import com.canteen.smile.modules.auth.service.DeviceSessionManagementService;
import com.canteen.smile.modules.auth.service.PlatformSessionService;
import com.canteen.smile.modules.auth.vo.DeviceSessionVO;
import com.canteen.smile.modules.auth.vo.SessionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

/** 当前设备会话查询和退出接口。 */
@RestController
@Validated
@RequiredArgsConstructor
public class SessionController {

    /** 平台设备会话服务。 */
    private final PlatformSessionService platformSessionService;

    /** 当前租户账号设备会话管理服务。 */
    private final DeviceSessionManagementService deviceSessionManagementService;

    /** @return 当前设备会话 */
    @GetMapping(AuthApiPaths.CURRENT_SESSION)
    public ApiResponse<SessionVO> currentSession() {
        return ApiResponse.success(platformSessionService.current());
    }

    /** @return 空数据成功响应 */
    @PostMapping(AuthApiPaths.LOGOUT)
    public ApiResponse<Void> logout() {
        platformSessionService.logoutCurrent();
        return ApiResponse.success(null);
    }

    /** @param query 分页参数 @return 当前租户账号有效设备会话。 */
    @GetMapping(AuthApiPaths.DEVICE_SESSIONS)
    public ApiResponse<PageResult<DeviceSessionVO>> deviceSessions(
            @Valid @ModelAttribute DeviceSessionPageQuery query
    ) {
        return ApiResponse.success(deviceSessionManagementService.page(query));
    }

    /** @param sessionId 设备会话 ID @param request 乐观锁命令。 */
    @DeleteMapping(AuthApiPaths.DEVICE_SESSIONS + "/{sessionId}")
    public ApiResponse<Void> logoutDevice(
            @PathVariable @Pattern(
                    regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
            ) String sessionId,
            @Valid @RequestBody LogoutDeviceSessionRequest request
    ) {
        deviceSessionManagementService.logoutSession(sessionId, request.version());
        return ApiResponse.success(null);
    }

    /** 下线当前设备以外的全部设备。 */
    @PostMapping(AuthApiPaths.LOGOUT_OTHER_DEVICE_SESSIONS)
    public ApiResponse<Void> logoutOtherDevices() {
        deviceSessionManagementService.logoutOthers();
        return ApiResponse.success(null);
    }
}
