package com.canteen.smile.modules.platform.controller;

import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.platform.dto.UsernameLoginResolutionRequest;
import com.canteen.smile.modules.platform.dto.MobileAccountLoginResolutionRequest;
import com.canteen.smile.modules.platform.service.PlatformLoginResolutionService;
import com.canteen.smile.modules.platform.vo.UsernameLoginResolutionVO;
import com.canteen.smile.modules.platform.vo.MobileAccountLoginCandidateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Auth 通过 HMAC 调用的登录主体解析接口。 */
@Validated
@RestController
@RequiredArgsConstructor
public class InternalLoginResolutionController {

    /** 平台登录解析服务。 */
    private final PlatformLoginResolutionService loginResolutionService;

    /**
     * 按用户名和应用入口解析登录主体。
     *
     * @param request 用户名解析请求
     * @return 不泄露外部账号存在性的内部解析结果
     */
    @PostMapping(IamApiPaths.USERNAME_LOGIN_RESOLUTION)
    public ApiResponse<UsernameLoginResolutionVO> resolve(
            @Valid @RequestBody UsernameLoginResolutionRequest request
    ) {
        return ApiResponse.success(loginResolutionService.resolve(request));
    }

    /**
     * 批量解析手机号登录候选账号，不接收手机号明文或摘要。
     *
     * @param request Auth 已验证的候选账号 ID
     * @return 当前仍可登录的候选账号快照
     */
    @PostMapping(IamApiPaths.MOBILE_ACCOUNT_LOGIN_RESOLUTION)
    public ApiResponse<java.util.List<MobileAccountLoginCandidateVO>> resolveMobileAccounts(
            @Valid @RequestBody MobileAccountLoginResolutionRequest request
    ) {
        return ApiResponse.success(loginResolutionService.resolveMobileAccounts(request));
    }
}
