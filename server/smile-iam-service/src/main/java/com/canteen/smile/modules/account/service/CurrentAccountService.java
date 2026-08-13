package com.canteen.smile.modules.account.service;

import com.canteen.smile.audit.annotation.AuditOperation;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.AuthTenantAccountClient;
import com.canteen.smile.modules.account.dto.ChangeUsernameRequest;
import com.canteen.smile.modules.account.mapper.TenantUserMapper;
import com.canteen.smile.modules.account.vo.ChangedUsernameVO;
import com.canteen.smile.modules.platform.service.UsernameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 当前租户账号自助资料服务。 */
@Service
@RequiredArgsConstructor
public class CurrentAccountService {

    /** 当前身份服务。 */
    private final TenantActorService actorService;
    /** 用户数据访问接口。 */
    private final TenantUserMapper mapper;
    /** 用户本地事务服务。 */
    private final TenantUserCommandService commandService;
    /** Auth 内部 Client。 */
    private final AuthTenantAccountClient authClient;

    /**
     * 修改当前账号用户名并使全部设备会话失效。
     *
     * @param request 修改请求
     * @return 修改结果
     */
    @AuditOperation(
            source = "IAM",
            categoryPath = {"租户端", "个人安全", "账号资料"},
            actionCode = "iam:me:username-change",
            actionName = "修改用户名",
            targetType = "TENANT_ACCOUNT",
            targetId = "#result.accountId",
            targetName = "#result.username",
            targetCode = "#result.username",
            reason = "#request.reason"
    )
    public ChangedUsernameVO changeUsername(ChangeUsernameRequest request) {
        TenantActorContext actor = actorService.current();
        String username = request.username().strip();
        String normalizedUsername = UsernameNormalizer.normalize(username);
        if (normalizedUsername.equals(UsernameNormalizer.normalize(actor.username()))) {
            throw new BusinessException("IAM_2810", "新用户名不能与当前用户名相同", 400);
        }
        if (mapper.countReservedUsername(normalizedUsername) > 0) {
            throw new BusinessException("IAM_2001", "用户名已经被当前或历史账号占用", 409);
        }
        authClient.consumeTenantReauthTicket(actor.accountId(), request.reauthTicket(), "TENANT_USERNAME_CHANGE");
        commandService.changeUsername(actor, username, normalizedUsername);
        return new ChangedUsernameVO(Long.toString(actor.accountId()), username);
    }
}
