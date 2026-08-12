package com.canteen.smile.audit.service;

import com.canteen.smile.audit.model.AuditActor;
import com.canteen.smile.audit.model.AuditEvent;
import com.canteen.smile.audit.model.AuditRecordCommand;
import com.canteen.smile.audit.spi.AuditEventPublisher;
import com.canteen.smile.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 注解切面与无登录态流程共用的审计事件构造和安全发布入口。 */
public class AuditRecorder {

    /** 当前类日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(AuditRecorder.class);

    /** 当前通用审计事件契约版本。 */
    private static final int SCHEMA_VERSION = 1;

    /** 可替换为 Outbox 或 MQ 的审计事件发布器。 */
    private final AuditEventPublisher eventPublisher;

    /** @param eventPublisher 审计事件发布边界 */
    public AuditRecorder(AuditEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 记录成功操作；存在事务同步时等待当前事务提交后发布。
     *
     * @param command 已包含后端可信操作人的审计声明
     * @param startedNanos 业务操作开始时的单调时钟纳秒值
     */
    public void recordSuccess(AuditRecordCommand command, long startedNanos) {
        record(command, "SUCCESS", null, startedNanos, true);
    }

    /**
     * 记录失败或拒绝操作并保留原始业务异常，事件立即安全发布。
     *
     * @param command 已包含后端可信操作人或明确匿名主体的审计声明
     * @param error 原始业务异常
     * @param startedNanos 业务操作开始时的单调时钟纳秒值
     */
    public void recordFailure(
            AuditRecordCommand command,
            Throwable error,
            long startedNanos
    ) {
        String failureCode = error instanceof BusinessException businessException
                ? businessException.getCode() : "SYSTEM_ERROR";
        String result = error instanceof BusinessException businessException
                && (businessException.getHttpStatus() == 401
                || businessException.getHttpStatus() == 403)
                ? "DENIED" : "FAILURE";
        record(command, result, failureCode, startedNanos, false);
    }

    /** 审计构造或发布失败不得改变原业务结果。 */
    private void record(
            AuditRecordCommand command,
            String result,
            String failureCode,
            long startedNanos,
            boolean afterCommit
    ) {
        try {
            AuditEvent event = buildEvent(
                    command,
                    result,
                    failureCode,
                    elapsedMillis(startedNanos)
            );
            if (afterCommit) {
                publishAfterCommit(event);
            } else {
                publishSafely(event);
            }
        } catch (RuntimeException exception) {
            log.error("Audit event construction failed, actionCode={}",
                    command == null ? null : command.actionCode(), exception);
        }
    }

    /** @return 由显式声明和运行时结果组成的不可变通用审计事件 */
    private AuditEvent buildEvent(
            AuditRecordCommand command,
            String result,
            String failureCode,
            long durationMs
    ) {
        if (command == null) {
            throw new IllegalArgumentException("Audit record command must not be null");
        }
        AuditActor actor = command.actor() == null ? AuditActor.system() : command.actor();
        String targetId = text(command.targetId());
        if (targetId == null) {
            targetId = Long.toString(actor.operatorId());
        }
        List<String> categoryPath = command.categoryPath() == null
                ? List.of()
                : command.categoryPath().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .toList();
        return new AuditEvent(
                UUID.randomUUID().toString(),
                SCHEMA_VERSION,
                required(command.source(), "source"),
                categoryPath,
                required(command.actionCode(), "actionCode"),
                required(command.actionName(), "actionName"),
                required(command.targetType(), "targetType"),
                targetId,
                text(command.targetName()),
                text(command.targetCode()),
                text(command.reason()),
                result,
                failureCode,
                text(command.maskedMobile()),
                text(command.loginMethod()),
                text(command.deviceSummary()),
                actor,
                MDC.get("traceId"),
                OffsetDateTime.now(),
                durationMs
        );
    }

    /** 成功事件等待当前事务提交，无事务时立即发布。 */
    private void publishAfterCommit(AuditEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        /** 业务事务提交成功后发布，回滚时不生成伪成功记录。 */
                        @Override
                        public void afterCommit() {
                            publishSafely(event);
                        }
                    }
            );
            return;
        }
        publishSafely(event);
    }

    /** 发布器故障仅写日志，不影响业务事务或返回值。 */
    private void publishSafely(AuditEvent event) {
        try {
            eventPublisher.publish(event);
        } catch (RuntimeException exception) {
            log.error("Audit event publish failed, eventId={}, actionCode={}",
                    event.eventId(), event.actionCode(), exception);
        }
    }

    /** @return 从业务开始时刻计算并限制为非负数的执行耗时毫秒值 */
    private long elapsedMillis(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    /** @return 去除首尾空白的可选文本 */
    private String text(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /** @return 非空必填文本 */
    private String required(String value, String field) {
        String normalized = text(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Audit command " + field + " must not be blank");
        }
        return normalized;
    }
}
