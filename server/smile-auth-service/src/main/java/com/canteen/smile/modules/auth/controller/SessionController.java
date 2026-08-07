package com.canteen.smile.modules.auth.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.auth.service.PlatformSessionService;
import com.canteen.smile.modules.auth.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前设备会话查询和退出接口。 */
@RestController
@RequiredArgsConstructor
public class SessionController {

    /** 平台设备会话服务。 */
    private final PlatformSessionService platformSessionService;

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
}
