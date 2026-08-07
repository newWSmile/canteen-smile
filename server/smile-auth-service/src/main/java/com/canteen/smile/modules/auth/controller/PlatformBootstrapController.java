package com.canteen.smile.modules.auth.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.auth.dto.PlatformBootstrapRequest;
import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;
import com.canteen.smile.modules.auth.service.PasswordEnvelopeService;
import com.canteen.smile.modules.auth.service.PlatformBootstrapService;
import com.canteen.smile.modules.auth.vo.PlatformBootstrapVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 首位平台超级管理员一次性引导接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(AuthApiPaths.PLATFORM_BOOTSTRAP)
public class PlatformBootstrapController {

    /** 外部一次性引导密钥请求头。 */
    private static final String BOOTSTRAP_SECRET_HEADER = "X-Bootstrap-Secret";

    /** 首位平台管理员引导服务。 */
    private final PlatformBootstrapService platformBootstrapService;

    /** 一次性密码信封解密服务。 */
    private final PasswordEnvelopeService passwordEnvelopeService;

    /**
     * 创建首位平台管理员，并且只在本次响应返回恢复码。
     *
     * @param bootstrapSecret 一次性高熵引导密钥
     * @param request 平台管理员资料和初始密码
     * @return 引导结果和一次性恢复码
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlatformBootstrapVO> bootstrap(
            @RequestHeader(BOOTSTRAP_SECRET_HEADER) String bootstrapSecret,
            @Valid @RequestBody PlatformBootstrapRequest request
    ) {
        /** 仅在当前初始化调用链中短暂存在的初始密码明文。 */
        String password = passwordEnvelopeService.decrypt(
                request.getPasswordEnvelope(),
                PasswordEnvelopePurpose.PLATFORM_BOOTSTRAP
        );
        return ApiResponse.success(platformBootstrapService.bootstrap(bootstrapSecret, request, password));
    }
}
