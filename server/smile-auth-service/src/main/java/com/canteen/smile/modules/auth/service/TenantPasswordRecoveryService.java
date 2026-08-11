package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.dto.SmsPasswordResetAccountSelectionRequest;
import com.canteen.smile.modules.auth.dto.SmsPasswordResetVerificationRequest;
import com.canteen.smile.modules.auth.entity.AccountSelectorTicketEntity;
import com.canteen.smile.modules.auth.model.AccountSelectorFlow;
import com.canteen.smile.modules.auth.service.TenantMobileAccountResolver.ResolvedCandidate;
import com.canteen.smile.modules.auth.vo.SmsPasswordResetResultVO;
import com.canteen.smile.modules.sms.model.SmsChallengeVerificationResult;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.modules.sms.service.SmsChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** 手机号验证码自助找回租户账号密码的分步编排服务。 */
@Service
@RequiredArgsConstructor
public class TenantPasswordRecoveryService {

    /** 多账号选择票据有效秒数。 */
    private static final long SELECTOR_TICKET_TTL_SECONDS = 300L;

    /** 手机号验证码挑战服务。 */
    private final SmsChallengeService smsChallengeService;

    /** 已验证手机号的租户账号候选解析服务。 */
    private final TenantMobileAccountResolver accountResolver;

    /** 账号选择票据事务服务。 */
    private final AccountSelectorTicketPersistenceService selectorTicketService;

    /** 租户账号密码重置票据与改密服务。 */
    private final TenantPasswordResetService passwordResetService;

    /**
     * 消费 PASSWORD_RESET 用途短信验证码；单账号直接签发重置票据，多账号返回选择候选。
     *
     * @param request 短信验证码校验请求
     * @return 设置新密码或账号选择结果
     */
    public SmsPasswordResetResultVO verify(SmsPasswordResetVerificationRequest request) {
        accountResolver.requireTenantApp(request.getAppCode());
        /** 已原子消费且用途确认为 PASSWORD_RESET 的短信验证结果。 */
        SmsChallengeVerificationResult verification = smsChallengeService.verifyAndConsume(
                request.getChallengeId(), request.getCode(), SmsPurpose.PASSWORD_RESET
        );
        /** 与已验证手机号绑定且在 IAM 中仍可用的租户账号。 */
        List<ResolvedCandidate> candidates = accountResolver.resolve(
                request.getAppCode(), verification.mobileHash()
        );
        if (candidates.isEmpty()) throw noAvailableAccount();
        if (candidates.size() == 1) {
            /** 单账号场景直接签发的短期密码重置票据。 */
            String resetTicket = passwordResetService.issueSmsSelfService(
                    candidates.get(0).identity().accountId(), null
            );
            return SmsPasswordResetResultVO.resetPassword(resetTicket);
        }
        /** 仅允许 PASSWORD_RESET 流程使用的五分钟账号选择票据。 */
        String selectorTicket = selectorTicketService.issue(
                verification.mobileHash(),
                accountResolver.candidateDigest(candidates),
                request.getAppCode(),
                AccountSelectorFlow.PASSWORD_RESET,
                OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).plusSeconds(
                        SELECTOR_TICKET_TTL_SECONDS
                )
        );
        return SmsPasswordResetResultVO.accountSelectionRequired(
                selectorTicket,
                candidates.stream().map(accountResolver::externalCandidate).toList()
        );
    }

    /**
     * 校验 PASSWORD_RESET 选择票据、候选集合完整性并为选中账号签发重置票据。
     *
     * @param request 多账号选择请求
     * @return 可以进入设置新密码页面的结果
     */
    public SmsPasswordResetResultVO selectAccount(
            SmsPasswordResetAccountSelectionRequest request
    ) {
        accountResolver.requireTenantApp(request.getAppCode());
        /** 用途、有效期和消费状态均有效的密码找回选择票据。 */
        AccountSelectorTicketEntity selectorTicket = selectorTicketService.requireActive(
                request.getAccountSelectorTicket(), AccountSelectorFlow.PASSWORD_RESET
        );
        if (!request.getAppCode().equals(selectorTicket.getAppCode())) {
            throw accountResolver.invalidSelectorTicket();
        }
        /** 当前手机号仍然可找回的最新账号候选集合。 */
        List<ResolvedCandidate> candidates = accountResolver.resolve(
                selectorTicket.getAppCode(), selectorTicket.getMobileHash()
        );
        if (!accountResolver.candidateDigest(candidates).equals(
                selectorTicket.getCandidateDigest()
        )) {
            throw accountResolver.invalidSelectorTicket();
        }
        /** 前端 bigint 字符串解析得到的目标账号 ID。 */
        long selectedAccountId = accountResolver.parseAccountId(request.getAccountId());
        /** 同时存在于原始摘要与当前 IAM 可用集合中的选中账号。 */
        ResolvedCandidate selected = candidates.stream()
                .filter(candidate -> candidate.identity().accountId() == selectedAccountId)
                .findFirst()
                .orElseThrow(accountResolver::invalidSelectorTicket);
        /** 为选中账号签发并与选择票据原子关联的密码重置票据。 */
        String resetTicket = passwordResetService.issueSmsSelfService(
                selected.identity().accountId(), selectorTicket
        );
        return SmsPasswordResetResultVO.resetPassword(resetTicket);
    }

    /** @return 手机号没有当前入口可找回账号的统一异常 */
    private BusinessException noAvailableAccount() {
        return new BusinessException("AUTH_1015", "该手机号没有当前入口可用的账号", 404);
    }
}
