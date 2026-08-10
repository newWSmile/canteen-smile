package com.canteen.smile.modules.auth.service;

import com.canteen.smile.modules.audit.mapper.AuthAuditLogMapper;
import com.canteen.smile.modules.auth.entity.DeviceSessionEntity;
import com.canteen.smile.modules.auth.mapper.DeviceSessionMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 设备会话与登录审计同事务写入契约测试。 */
class DeviceSessionPersistenceServiceTest {

    /** 验证密码会话创建时同步写入成功登录审计。 */
    @Test
    void shouldInsertSessionAndPasswordLoginAuditTogether() {
        DeviceSessionMapper sessionMapper = mock(DeviceSessionMapper.class);
        AuthAuditLogMapper auditMapper = mock(AuthAuditLogMapper.class);
        DeviceSessionEntity entity = session("PASSWORD");
        when(sessionMapper.insertDeviceSession(entity)).thenReturn(1);
        when(auditMapper.insertSessionCreatedAudit(
                entity, "auth:login:password", "用户名密码登录",
                "audit_user", "审计用户", null)).thenReturn(1);
        DeviceSessionPersistenceService service = new DeviceSessionPersistenceService(sessionMapper, auditMapper);

        service.create(entity, "audit_user", "审计用户");

        verify(sessionMapper).insertDeviceSession(entity);
        verify(auditMapper).insertSessionCreatedAudit(
                entity, "auth:login:password", "用户名密码登录",
                "audit_user", "审计用户", null);
    }

    /** 验证审计写入失败会让事务方法失败，防止静默遗漏。 */
    @Test
    void shouldFailWhenLoginAuditCannotBeInserted() {
        DeviceSessionMapper sessionMapper = mock(DeviceSessionMapper.class);
        AuthAuditLogMapper auditMapper = mock(AuthAuditLogMapper.class);
        DeviceSessionEntity entity = session("RECOVERY_CODE");
        when(sessionMapper.insertDeviceSession(entity)).thenReturn(1);
        when(auditMapper.insertSessionCreatedAudit(
                entity, "auth:login:recovery-code", "恢复码登录",
                "audit_user", "审计用户", null)).thenReturn(0);
        DeviceSessionPersistenceService service = new DeviceSessionPersistenceService(sessionMapper, auditMapper);

        assertThatThrownBy(() -> service.create(entity, "audit_user", "审计用户"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Auth login audit was not inserted");
    }

    /** @param loginMethod 登录方式 @return 测试设备会话 */
    private DeviceSessionEntity session(String loginMethod) {
        DeviceSessionEntity entity = new DeviceSessionEntity();
        entity.setSubjectType("PLATFORM_IDENTITY");
        entity.setSubjectId(1L);
        entity.setLoginMethod(loginMethod);
        return entity;
    }
}
