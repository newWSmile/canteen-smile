package com.canteen.smile.modules.auth.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.auth.dto.PasswordReauthRequest;
import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;
import com.canteen.smile.modules.auth.service.PasswordEnvelopeService;
import com.canteen.smile.modules.auth.service.PlatformPasswordReauthService;
import com.canteen.smile.modules.auth.vo.ReauthTicketVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 当前登录身份敏感操作密码再认证接口。 */
@RestController
@RequiredArgsConstructor
public class PasswordReauthController {

    /** 密码信封解密服务。 */
    private final PasswordEnvelopeService passwordEnvelopeService;

    /** 平台密码再认证服务。 */
    private final PlatformPasswordReauthService reauthService;

    /**
     * 使用当前密码签发绑定单一敏感操作的五分钟一次性再认证票据。
     *
     * @param request 加密密码及允许操作
     * @return 再认证票据
     */
    @PostMapping(AuthApiPaths.PASSWORD_REAUTH)
    public ApiResponse<ReauthTicketVO> reauthenticate(@Valid @RequestBody PasswordReauthRequest request) {
        /** 仅在当前调用栈短暂存在的当前密码明文。 */
        PasswordEnvelopePurpose purpose = reauthService.currentPasswordPurpose();
        String password = passwordEnvelopeService.decrypt(request.passwordEnvelope(), purpose);
        return ApiResponse.success(reauthService.issue(password, request.allowedAction()));
    }
}
