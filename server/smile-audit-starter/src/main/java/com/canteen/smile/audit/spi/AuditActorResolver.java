package com.canteen.smile.audit.spi;

import com.canteen.smile.audit.model.AuditActor;

/** 在当前业务线程中解析真实登录人，禁止异步线程重新读取认证上下文。 */
@FunctionalInterface
public interface AuditActorResolver {

    /** @return 当前登录人完整审计快照；无登录人时返回明确系统或匿名身份 */
    AuditActor resolve();
}
