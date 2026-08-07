package com.canteen.smile.modules.platform.controller;

import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.platform.dto.BootstrapPlatformIdentityRequest;
import com.canteen.smile.modules.platform.service.PlatformIdentityBootstrapService;
import com.canteen.smile.modules.platform.vo.PlatformIdentityInternalVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Auth 通过 HMAC 调用的平台身份内部接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.INTERNAL_V1 + "/platform-identities")
public class InternalPlatformIdentityController {

    /** 首位平台身份事务服务。 */
    private final PlatformIdentityBootstrapService bootstrapService;

    /**
     * 幂等创建首位平台身份。
     *
     * @param request 首位平台身份资料
     * @return 平台身份内部契约
     */
    @PostMapping("/bootstrap")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlatformIdentityInternalVO> bootstrap(
            @Valid @RequestBody BootstrapPlatformIdentityRequest request
    ) {
        return ApiResponse.success(bootstrapService.bootstrap(request));
    }

    /**
     * Auth 凭证就绪后激活平台身份。
     *
     * @param identityId 平台身份 ID
     * @return 激活后的平台身份
     */
    @PostMapping("/{identityId}/actions/activate")
    public ApiResponse<PlatformIdentityInternalVO> activate(
            @Positive @PathVariable long identityId
    ) {
        return ApiResponse.success(bootstrapService.activate(identityId));
    }
}
