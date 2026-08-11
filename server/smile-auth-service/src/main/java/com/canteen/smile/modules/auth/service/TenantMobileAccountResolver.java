package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.internal.client.IamPlatformIdentityClient;
import com.canteen.smile.internal.client.dto.MobileAccountLoginCandidateInternalResponse;
import com.canteen.smile.internal.client.dto.MobileAccountLoginResolutionInternalRequest;
import com.canteen.smile.modules.auth.mapper.DeviceSessionMapper;
import com.canteen.smile.modules.auth.mapper.MobileBindingMapper;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.vo.MobileLoginCandidateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 根据已验证手机号解析当前应用入口可用租户账号的共享服务。 */
@Service
@RequiredArgsConstructor
public class TenantMobileAccountResolver {

    /** 手机号绑定数据访问接口。 */
    private final MobileBindingMapper mobileBindingMapper;

    /** IAM 登录候选解析 Client。 */
    private final IamPlatformIdentityClient iamClient;

    /** 设备会话数据访问接口。 */
    private final DeviceSessionMapper deviceSessionMapper;

    /**
     * 批量解析并按最近登录时间、用户名和账号 ID 稳定排序。
     *
     * @param appCode 租户应用入口
     * @param mobileHash 已验证手机号摘要
     * @return Auth 绑定与 IAM 状态双重校验后的账号候选
     */
    public List<ResolvedCandidate> resolve(String appCode, String mobileHash) {
        requireTenantApp(appCode);
        /** Auth 中已完成验证且仍有效的手机号绑定账号。 */
        List<Long> boundAccountIds =
                mobileBindingMapper.selectVerifiedTenantAccountIdsByMobileHash(mobileHash);
        if (boundAccountIds.isEmpty()) return List.of();
        /** 经 IAM 租户、机构及账号状态最终校验后的身份快照。 */
        List<MobileAccountLoginCandidateInternalResponse> identities = iamClient.resolveMobileAccounts(
                new MobileAccountLoginResolutionInternalRequest(appCode, boundAccountIds)
        );
        if (identities.isEmpty()) return List.of();
        /** 仅包含 IAM 确认可用账号的批量会话查询参数。 */
        List<Long> resolvedAccountIds = identities.stream()
                .map(MobileAccountLoginCandidateInternalResponse::accountId)
                .toList();
        /** 账号 ID 到最近一次登录时间的映射。 */
        Map<Long, OffsetDateTime> latestLogins = new HashMap<>();
        for (DeviceSessionMapper.LatestLoginRow row
                : deviceSessionMapper.selectLatestTenantLogins(resolvedAccountIds)) {
            latestLogins.put(row.accountId(), row.latestLoginTime());
        }
        /** 合并 IAM 身份快照与 Auth 最近登录时间的候选集合。 */
        List<ResolvedCandidate> candidates = new ArrayList<>(identities.size());
        for (MobileAccountLoginCandidateInternalResponse identity : identities) {
            candidates.add(new ResolvedCandidate(identity, latestLogins.get(identity.accountId())));
        }
        candidates.sort(
                Comparator.comparing(
                                ResolvedCandidate::latestLoginTime,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(candidate -> candidate.identity().username(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(candidate -> candidate.identity().accountId())
        );
        return List.copyOf(candidates);
    }

    /** @param candidates 当前候选 @return 与候选顺序无关的账号 ID 集合摘要 */
    public String candidateDigest(List<ResolvedCandidate> candidates) {
        /** 与候选展示顺序无关的升序账号 ID 串。 */
        String canonicalIds = candidates.stream()
                .map(candidate -> candidate.identity().accountId())
                .sorted()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return HmacRequestSigner.sha256Hex(canonicalIds.getBytes(StandardCharsets.UTF_8));
    }

    /** @param candidate 内部候选 @return 不含会话策略的前端安全投影 */
    public MobileLoginCandidateVO externalCandidate(ResolvedCandidate candidate) {
        /** 经过 IAM 校验且可安全向当前手机号持有人展示的身份快照。 */
        MobileAccountLoginCandidateInternalResponse identity = candidate.identity();
        return new MobileLoginCandidateVO(
                Long.toString(identity.accountId()),
                identity.tenantName(),
                identity.organizationName(),
                identity.username(),
                identity.displayName(),
                candidate.latestLoginTime()
        );
    }

    /** @param appCode 应用入口编码 */
    public void requireTenantApp(String appCode) {
        if (!AuthConstants.TENANT_ADMIN_APP.equals(appCode)
                && !AuthConstants.TENANT_PORTAL_APP.equals(appCode)) {
            throw new BusinessException("AUTH_1008", "当前应用入口不匹配", 403);
        }
    }

    /** @param accountId 前端 bigint 字符串 @return 正数账号 ID */
    public long parseAccountId(String accountId) {
        try {
            /** 从 JSON bigint 字符串解析得到的账号 ID。 */
            long value = Long.parseLong(accountId);
            if (value <= 0L) throw invalidSelectorTicket();
            return value;
        } catch (NumberFormatException exception) {
            throw invalidSelectorTicket();
        }
    }

    /** @return 不泄露票据内部状态的统一异常 */
    public BusinessException invalidSelectorTicket() {
        return new BusinessException("AUTH_1014", "账号选择凭证无效或已过期", 400);
    }

    /** IAM 安全账号快照和 Auth 最近登录时间。 */
    public record ResolvedCandidate(
            MobileAccountLoginCandidateInternalResponse identity,
            OffsetDateTime latestLoginTime
    ) {
    }
}
