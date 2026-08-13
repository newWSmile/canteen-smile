package com.canteen.smile.modules.account.controller;

import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.account.dto.ChangeUsernameRequest;
import com.canteen.smile.modules.account.service.CurrentAccountService;
import com.canteen.smile.modules.account.vo.ChangedUsernameVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前租户账号自助资料接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.CURRENT_ACCOUNT)
public class CurrentAccountController {

    /** 当前账号服务。 */
    private final CurrentAccountService service;

    /** @param request 用户名修改请求 @return 修改后的用户名 */
    @PostMapping("/username/actions/change")
    public ApiResponse<ChangedUsernameVO> changeUsername(@Valid @RequestBody ChangeUsernameRequest request) {
        return ApiResponse.success(service.changeUsername(request));
    }
}
