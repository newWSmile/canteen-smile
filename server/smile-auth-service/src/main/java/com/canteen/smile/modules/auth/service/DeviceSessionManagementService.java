package com.canteen.smile.modules.auth.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.audit.annotation.AuditOperation;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.dto.DeviceSessionPageQuery;
import com.canteen.smile.modules.auth.entity.DeviceSessionEntity;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.vo.DeviceSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 当前租户账号查询和下线本人设备会话的服务。 */
@Service
@RequiredArgsConstructor
public class DeviceSessionManagementService {

    /** 当前认证主体解析服务。 */
    private final CurrentAuthSubjectService subjectService;
    /** 设备会话数据库事务服务。 */
    private final DeviceSessionPersistenceService persistenceService;

    /** @param query 分页参数 @return 当前账号有效设备会话分页。 */
    public PageResult<DeviceSessionVO> page(DeviceSessionPageQuery query) {
        CurrentAuthSubjectService.CurrentTenantSubject subject = subjectService.currentTenant();
        String currentSessionId = currentBusinessSessionId();
        PageResult<DeviceSessionEntity> page = persistenceService.page(
                subject.tenantId(), subject.accountId(), query
        );
        List<DeviceSessionVO> items = page.items().stream()
                .map(entity -> toVO(entity, currentSessionId))
                .toList();
        return new PageResult<>(items, page.pageNo(), page.pageSize(), page.total());
    }

    /**
     * 下线当前账号指定设备。
     *
     * @param sessionId 业务设备会话 ID
     * @param version 乐观锁版本
     */
    @AuditOperation(
            source = "AUTH",
            categoryPath = {"租户端", "账号安全", "设备会话"},
            actionCode = "auth:session:logout-device",
            actionName = "下线指定设备",
            targetType = "DEVICE_SESSION",
            targetId = "#sessionId"
    )
    public void logoutSession(String sessionId, long version) {
        CurrentAuthSubjectService.CurrentTenantSubject subject = subjectService.currentTenant();
        persistenceService.invalidateOwnedSession(
                subject.tenantId(), subject.accountId(), sessionId, version
        );
        String tokenValue = activeTokensBySessionId(subject.accountId()).get(sessionId);
        if (tokenValue != null) {
            StpUtil.logoutByTokenValue(tokenValue);
        }
    }

    /** 下线当前请求设备以外的全部有效设备。 */
    @AuditOperation(
            source = "AUTH",
            categoryPath = {"租户端", "账号安全", "设备会话"},
            actionCode = "auth:session:logout-others",
            actionName = "下线其他全部设备",
            targetType = "TENANT_ACCOUNT",
            targetId = "#actor.operatorId",
            targetName = "#actor.displayName",
            targetCode = "#actor.username"
    )
    public void logoutOthers() {
        CurrentAuthSubjectService.CurrentTenantSubject subject = subjectService.currentTenant();
        String currentSessionId = currentBusinessSessionId();
        Map<String, String> activeTokens = activeTokensBySessionId(subject.accountId());
        persistenceService.invalidateOtherSessions(
                subject.tenantId(), subject.accountId(), currentSessionId
        );
        activeTokens.forEach((sessionId, tokenValue) -> {
            if (!currentSessionId.equals(sessionId)) {
                StpUtil.logoutByTokenValue(tokenValue);
            }
        });
    }

    /** @return 当前 Token Session 中的业务会话 ID。 */
    private String currentBusinessSessionId() {
        Object value = StpUtil.getTokenSession().get(AuthConstants.TOKEN_BUSINESS_SESSION_ID_ATTRIBUTE);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BusinessException("AUTH_1006", "会话已因安全变化失效", 401);
        }
        return String.valueOf(value);
    }

    /**
     * 从 Sa-Token 当前账号最多一百个有效终端解析业务会话与 Token 的对应关系。
     * 数据库始终只保存 Token 摘要，不持久化或输出明文 Token。
     */
    private Map<String, String> activeTokensBySessionId(long accountId) {
        Map<String, String> tokens = new LinkedHashMap<>();
        List<SaTerminalInfo> terminals = StpUtil.getTerminalListByLoginId(
                AuthConstants.TENANT_LOGIN_PREFIX + accountId
        );
        for (SaTerminalInfo terminal : terminals.stream().limit(100).toList()) {
            SaSession tokenSession = StpUtil.getStpLogic()
                    .getTokenSessionByToken(terminal.getTokenValue(), false);
            if (tokenSession == null) {
                continue;
            }
            Object sessionId = tokenSession.get(AuthConstants.TOKEN_BUSINESS_SESSION_ID_ATTRIBUTE);
            if (sessionId != null) {
                tokens.put(String.valueOf(sessionId), terminal.getTokenValue());
            }
        }
        return tokens;
    }

    /** 将会话实体转换为脱敏外部视图。 */
    private DeviceSessionVO toVO(DeviceSessionEntity entity, String currentSessionId) {
        return new DeviceSessionVO(
                entity.getSessionId(), entity.getAppCode(), entity.getDeviceType(), entity.getDeviceName(),
                entity.getLoginMethod(), entity.getLoginIpMasked(), entity.getLoginTime(),
                entity.getLastActiveTime(), entity.getIdleExpiresAt(), entity.getAbsoluteExpiresAt(),
                entity.getSessionId().equals(currentSessionId), entity.getVersion()
        );
    }
}
