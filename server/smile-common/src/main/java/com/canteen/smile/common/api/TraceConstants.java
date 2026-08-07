package com.canteen.smile.common.api;

/** 请求链路常量。 */
public final class TraceConstants {

    /** MDC 中的链路标识名称。 */
    public static final String TRACE_ID = "traceId";

    /** HTTP 响应头中的链路标识名称。 */
    public static final String TRACE_HEADER = "X-Trace-Id";

    private TraceConstants() {
    }
}
