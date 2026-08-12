package com.canteen.smile.audit.spi;

/** 在原业务请求线程中解析由入口网关确认的完整客户端 IP。 */
@FunctionalInterface
public interface AuditClientIpResolver {

    /** @return 当前请求的完整客户端 IP；非 HTTP 任务或无法确认时为空 */
    String resolve();
}
