package com.canteen.smile.modules.platform.service;

import com.canteen.smile.modules.platform.dto.UsernameLoginResolutionRequest;
import com.canteen.smile.modules.platform.dto.MobileAccountLoginResolutionRequest;
import com.canteen.smile.modules.platform.entity.PlatformIdentityEntity;
import com.canteen.smile.modules.platform.mapper.PlatformIdentityMapper;
import com.canteen.smile.modules.platform.model.PlatformIdentityStatus;
import com.canteen.smile.modules.platform.vo.UsernameLoginResolutionVO;
import com.canteen.smile.modules.platform.vo.MobileAccountLoginCandidateVO;
import com.canteen.smile.modules.account.mapper.AccountLifecycleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 平台用户名登录主体解析服务。 */
@Service
@RequiredArgsConstructor
public class PlatformLoginResolutionService {

    /** 平台身份数据访问接口。 */
    private final PlatformIdentityMapper platformIdentityMapper;

    /** 租户账号登录上下文数据访问接口。 */
    private final AccountLifecycleMapper accountLifecycleMapper;

    /**
     * 按应用入口解析平台身份或租户账号。
     *
     * @param request 用户名和应用入口
     * @return 内部登录解析结果
     */
    @Transactional(readOnly = true)
    public UsernameLoginResolutionVO resolve(UsernameLoginResolutionRequest request) {
        /** 按统一规则归一化后的用户名。 */
        String normalizedUsername = UsernameNormalizer.normalize(request.username());
        if ("TENANT_ADMIN".equals(request.appCode()) || "TENANT_PORTAL".equals(request.appCode())) {
            return resolveTenantAccount(normalizedUsername);
        }
        if (!"PLATFORM_ADMIN".equals(request.appCode())) {
            return UsernameLoginResolutionVO.unresolved();
        }
        /** IAM 平台身份。 */
        PlatformIdentityEntity identity = platformIdentityMapper.selectByNormalizedUsername(normalizedUsername);
        if (identity == null
                || Boolean.TRUE.equals(identity.getDeleted())
                || !PlatformIdentityStatus.ACTIVE.name().equals(identity.getStatus())) {
            return UsernameLoginResolutionVO.unresolved();
        }
        return new UsernameLoginResolutionVO(
                true,
                "PLATFORM_IDENTITY",
                identity.getId().toString(),
                identity.getUsername(),
                identity.getDisplayName() == null ? identity.getUsername() : identity.getDisplayName(),
                identity.getStatus(),
                identity.getAuthzVersion(),
                null, null, null, null, null, null, null, null, null
        );
    }

    /**
     * 批量解析已由 Auth 手机号摘要命中的可登录租户账号。
     *
     * @param request 应用入口和账号 ID 集合
     * @return 当前仍可登录的候选账号快照
     */
    @Transactional(readOnly = true)
    public List<MobileAccountLoginCandidateVO> resolveMobileAccounts(
            MobileAccountLoginResolutionRequest request
    ) {
        return accountLifecycleMapper.selectMobileLoginCandidates(request.accountIds()).stream()
                .map(row -> new MobileAccountLoginCandidateVO(
                        row.accountId(), row.tenantId(), row.tenantName(),
                        row.organizationId(), row.organizationName(), row.username(),
                        row.displayName() == null ? row.username() : row.displayName(),
                        row.authzVersion(), row.concurrentLoginEnabled(), row.maxDevices(),
                        row.rememberMeEnabled(), row.idleSeconds(), row.absoluteSeconds(),
                        row.rememberIdleSeconds(), row.rememberAbsoluteSeconds()
                ))
                .toList();
    }

    /** @param normalizedUsername 归一化用户名 @return 可登录租户账号快照 */
    private UsernameLoginResolutionVO resolveTenantAccount(String normalizedUsername) {
        AccountLifecycleMapper.LoginContextRow account = accountLifecycleMapper.selectLoginContext(normalizedUsername);
        if (account == null
                || !"ACTIVE".equals(account.accountStatus())
                || !"ACTIVE".equals(account.tenantStatus())
                || !"ACTIVE".equals(account.organizationStatus())) {
            return UsernameLoginResolutionVO.unresolved();
        }
        return new UsernameLoginResolutionVO(
                true,
                "TENANT_ACCOUNT",
                Long.toString(account.accountId()),
                account.username(),
                account.displayName() == null ? account.username() : account.displayName(),
                account.accountStatus(),
                account.authzVersion(),
                Long.toString(account.tenantId()),
                Long.toString(account.organizationId()),
                account.concurrentLoginEnabled(),
                account.maxDevices(),
                account.rememberMeEnabled(),
                account.idleSeconds(),
                account.absoluteSeconds(),
                account.rememberIdleSeconds(),
                account.rememberAbsoluteSeconds()
        );
    }
}
