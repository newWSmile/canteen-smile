package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.audit.mapper.AuthAuditLogMapper;
import com.canteen.smile.modules.auth.entity.MobileBindingEntity;
import com.canteen.smile.modules.auth.mapper.MobileBindingMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 手机号绑定查询、唯一写入和安全审计的本地事务边界。 */
@Service
@RequiredArgsConstructor
public class MobileBindingPersistenceService {

    /** 当前账号已经绑定手机号的错误码。 */
    private static final String ALREADY_BOUND_CODE = "AUTH_1301";

    /** 手机号绑定数据访问接口。 */
    private final MobileBindingMapper mobileBindingMapper;

    /** Auth 安全审计数据访问接口。 */
    private final AuthAuditLogMapper auditLogMapper;

    /** @param subjectType 主体类型 @param subjectId 主体 ID @return 当前有效绑定 */
    @Transactional(readOnly = true)
    public MobileBindingEntity findVerified(String subjectType, long subjectId) {
        return mobileBindingMapper.selectVerifiedBySubject(subjectType, subjectId);
    }

    /**
     * 原子写入首次绑定和成功审计。
     *
     * @param entity 已验证并加密的手机号绑定
     * @param subject 当前可信租户账号
     */
    @Transactional
    public void bind(
            MobileBindingEntity entity,
            CurrentAuthSubjectService.CurrentTenantSubject subject
    ) {
        if (mobileBindingMapper.insertVerified(entity) != 1) {
            throw new BusinessException(ALREADY_BOUND_CODE, "当前账号已经绑定手机号", 409);
        }
        if (auditLogMapper.insertMobileBindingAudit(
                subject.tenantId(),
                subject.accountId(),
                subject.username(),
                subject.displayName(),
                entity.getMaskedMobile(),
                MDC.get("traceId")
        ) != 1) {
            throw new IllegalStateException("Mobile binding audit was not inserted");
        }
    }
}
