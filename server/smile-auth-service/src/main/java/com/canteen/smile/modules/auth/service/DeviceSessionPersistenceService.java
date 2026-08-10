package com.canteen.smile.modules.auth.service;

import com.canteen.smile.modules.auth.entity.DeviceSessionEntity;
import com.canteen.smile.modules.auth.mapper.DeviceSessionMapper;
import com.canteen.smile.modules.audit.mapper.AuthAuditLogMapper;
import com.canteen.smile.modules.audit.model.AuthLoginAuditAction;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设备会话数据库事务边界。 */
@Service
@RequiredArgsConstructor
public class DeviceSessionPersistenceService {

    /** 设备会话数据访问接口。 */
    private final DeviceSessionMapper deviceSessionMapper;

    /** Auth 登录审计数据访问接口。 */
    private final AuthAuditLogMapper auditLogMapper;

    /**
     * @param entity 设备会话实体
     * @param username 已验证主体用户名
     * @param displayName 已验证主体显示名称
     */
    @Transactional
    public void create(DeviceSessionEntity entity, String username, String displayName) {
        if (deviceSessionMapper.insertDeviceSession(entity) != 1) {
            throw new IllegalStateException("Device session was not inserted");
        }
        /** 与已确认登录方式对应的领域审计动作。 */
        AuthLoginAuditAction action = AuthLoginAuditAction.fromLoginMethod(entity.getLoginMethod());
        if (auditLogMapper.insertSessionCreatedAudit(
                entity, action.actionCode(), action.actionName(), username, displayName, MDC.get("traceId")
        ) != 1) {
            throw new IllegalStateException("Auth login audit was not inserted");
        }
    }

    /** @param sessionId 会话 ID @return 当前有效设备会话 */
    @Transactional(readOnly = true)
    public DeviceSessionEntity findActive(String sessionId) {
        return deviceSessionMapper.selectActiveBySessionId(sessionId);
    }

    /** @param sessionId 会话 ID */
    @Transactional
    public void markLoggedOut(String sessionId) {
        deviceSessionMapper.markLoggedOut(sessionId);
    }
}
