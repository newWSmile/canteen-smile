package com.canteen.smile.audit.spi;

import com.canteen.smile.audit.model.AuditEvent;

/** 审计事件发布边界；未来接入 MQ 时只替换该接口实现。 */
@FunctionalInterface
public interface AuditEventPublisher {

    /** @param event 已脱敏且包含登录人快照的不可变审计事件 */
    void publish(AuditEvent event);
}
