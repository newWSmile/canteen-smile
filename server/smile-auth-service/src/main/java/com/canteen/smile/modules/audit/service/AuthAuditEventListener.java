package com.canteen.smile.modules.audit.service;

import com.canteen.smile.audit.model.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** 使用项目有界业务线程池异步消费 Auth 本地审计事件。 */
@Component
@RequiredArgsConstructor
public class AuthAuditEventListener {

    /** 当前类日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(AuthAuditEventListener.class);

    /** Auth 异步审计落库服务。 */
    private final AuthAsyncAuditWriter writer;

    /** @param event 通用审计事件；其它来源事件不由 Auth 数据库消费 */
    @Async("applicationTaskExecutor")
    @EventListener
    public void onAuditEvent(AuditEvent event) {
        if (!"AUTH".equals(event.source())) {
            return;
        }
        try {
            writer.write(event);
        } catch (RuntimeException exception) {
            log.error("Auth async audit persistence failed, eventId={}, actionCode={}",
                    event.eventId(), event.actionCode(), exception);
        }
    }
}
