package com.canteen.smile.modules.auth.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.auth.dto.PasswordEncryptionChallengeRequest;
import com.canteen.smile.modules.auth.service.PasswordEnvelopeService;
import com.canteen.smile.modules.auth.vo.PasswordEncryptionChallengeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 向匿名认证流程签发一次性短期密码加密挑战。 */
@Validated
@RestController
@RequiredArgsConstructor
public class PasswordEncryptionController {

    /** 密码信封挑战及解密服务。 */
    private final PasswordEnvelopeService passwordEnvelopeService;

    /**
     * 创建绑定业务用途且只能消费一次的短期 RSA 公钥挑战。
     *
     * @param request 挑战用途
     * @return 短期公钥、nonce、时间戳和失效时间
     */
    @PostMapping(AuthApiPaths.PASSWORD_ENCRYPTION_CHALLENGES)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PasswordEncryptionChallengeVO> createChallenge(
            @Valid @RequestBody PasswordEncryptionChallengeRequest request
    ) {
        return ApiResponse.success(passwordEnvelopeService.issue(request.purpose()));
    }
}
