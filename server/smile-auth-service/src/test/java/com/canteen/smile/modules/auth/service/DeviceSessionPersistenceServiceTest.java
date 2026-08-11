package com.canteen.smile.modules.auth.service;

import com.canteen.smile.modules.auth.entity.DeviceSessionEntity;
import com.canteen.smile.modules.auth.mapper.DeviceSessionMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 设备会话独立事务写入契约测试。 */
class DeviceSessionPersistenceServiceTest {

    /** 验证设备会话由独立事务服务写入。 */
    @Test
    void shouldInsertSession() {
        DeviceSessionMapper sessionMapper = mock(DeviceSessionMapper.class);
        DeviceSessionEntity entity = session("PASSWORD");
        when(sessionMapper.insertDeviceSession(entity)).thenReturn(1);
        DeviceSessionPersistenceService service = new DeviceSessionPersistenceService(sessionMapper);

        service.create(entity);

        verify(sessionMapper).insertDeviceSession(entity);
    }

    /** 验证设备会话写入失败时抛出明确异常。 */
    @Test
    void shouldFailWhenSessionCannotBeInserted() {
        DeviceSessionMapper sessionMapper = mock(DeviceSessionMapper.class);
        DeviceSessionEntity entity = session("RECOVERY_CODE");
        when(sessionMapper.insertDeviceSession(entity)).thenReturn(0);
        DeviceSessionPersistenceService service = new DeviceSessionPersistenceService(sessionMapper);

        assertThatThrownBy(() -> service.create(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Device session was not inserted");
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
