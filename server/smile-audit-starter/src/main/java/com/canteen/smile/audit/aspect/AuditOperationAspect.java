package com.canteen.smile.audit.aspect;

import com.canteen.smile.audit.annotation.AuditOperation;
import com.canteen.smile.audit.expression.AuditExpressionEvaluator;
import com.canteen.smile.audit.model.AuditActor;
import com.canteen.smile.audit.model.AuditEvent;
import com.canteen.smile.audit.spi.AuditActorResolver;
import com.canteen.smile.audit.spi.AuditEventPublisher;
import com.canteen.smile.common.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** 拦截审计注解并在业务提交后发布不影响主事务的异步事件。 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class AuditOperationAspect {

    /** 当前类安全日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(AuditOperationAspect.class);

    /** 当前通用事件契约版本。 */
    private static final int SCHEMA_VERSION = 1;

    /** 当前服务的登录人解析器。 */
    private final AuditActorResolver actorResolver;

    /** 可替换为 MQ 的审计事件发布器。 */
    private final AuditEventPublisher eventPublisher;

    /** 注解字段表达式求值器。 */
    private final AuditExpressionEvaluator expressionEvaluator;

    /**
     * 创建审计切面。
     *
     * @param actorResolver 登录人解析器
     * @param eventPublisher 审计事件发布器
     * @param expressionEvaluator 显式字段表达式求值器
     */
    public AuditOperationAspect(
            AuditActorResolver actorResolver,
            AuditEventPublisher eventPublisher,
            AuditExpressionEvaluator expressionEvaluator
    ) {
        this.actorResolver = actorResolver;
        this.eventPublisher = eventPublisher;
        this.expressionEvaluator = expressionEvaluator;
    }

    /**
     * 在调用线程捕获登录人，自动区分成功、拒绝和失败并保持原始返回或异常。
     *
     * @param joinPoint 被调用的公共 Service 方法
     * @param operation 审计声明
     * @return 原业务方法返回值
     * @throws Throwable 原业务方法异常
     */
    @Around("@annotation(operation)")
    public Object around(ProceedingJoinPoint joinPoint, AuditOperation operation) throws Throwable {
        /** 异步切换前从当前 Sa-Token 和服务数据库取得的登录人快照。 */
        AuditActor actor = resolveActor();
        /** 业务方法开始的单调时钟纳秒值。 */
        long startedNanos = System.nanoTime();
        /** 当前被代理对象上的最具体实现方法。 */
        Method method = specificMethod(joinPoint);
        try {
            /** 原业务方法返回值。 */
            Object result = joinPoint.proceed();
            recordSuccess(operation, method, joinPoint.getArgs(), result, actor, startedNanos);
            return result;
        } catch (Throwable error) {
            recordFailure(operation, method, joinPoint.getArgs(), error, actor, startedNanos);
            throw error;
        }
    }

    /** 审计构造或发布失败只能记录日志，绝不改变已经成功的业务返回。 */
    private void recordSuccess(
            AuditOperation operation,
            Method method,
            Object[] arguments,
            Object result,
            AuditActor actor,
            long startedNanos
    ) {
        try {
            AuditEvent event = buildEvent(
                    operation, method, arguments, result, null, actor,
                    "SUCCESS", null, startedNanos
            );
            publishAfterCommit(event);
        } catch (RuntimeException exception) {
            log.error("Audit success event construction failed for {}.{}",
                    method.getDeclaringClass().getSimpleName(), method.getName(), exception);
        }
    }

    /** 审计失败记录本身不得遮蔽或替换原始业务异常。 */
    private void recordFailure(
            AuditOperation operation,
            Method method,
            Object[] arguments,
            Throwable error,
            AuditActor actor,
            long startedNanos
    ) {
        if (!operation.recordFailure()) {
            return;
        }
        try {
            String failureCode = error instanceof BusinessException businessException
                    ? businessException.getCode() : "SYSTEM_ERROR";
            String auditResult = error instanceof BusinessException businessException
                    && (businessException.getHttpStatus() == 401
                    || businessException.getHttpStatus() == 403)
                    ? "DENIED" : "FAILURE";
            publishSafely(buildEvent(
                    operation, method, arguments, null, error, actor,
                    auditResult, failureCode, startedNanos
            ));
        } catch (RuntimeException exception) {
            log.error("Audit failure event construction failed for {}.{}",
                    method.getDeclaringClass().getSimpleName(), method.getName(), exception);
        }
    }

    /** 从当前服务解析登录人，解析异常时显式降级为系统主体且不阻断业务。 */
    private AuditActor resolveActor() {
        try {
            /** 当前服务解析出的真实登录人快照。 */
            AuditActor actor = actorResolver.resolve();
            return actor == null ? AuditActor.system() : actor;
        } catch (RuntimeException exception) {
            log.warn("Audit actor resolution failed: {}", exception.getClass().getSimpleName());
            return AuditActor.system();
        }
    }

    /** 找到实现类上用于参数名和注解表达式解析的具体方法。 */
    private Method specificMethod(ProceedingJoinPoint joinPoint) {
        /** 代理连接点暴露的方法签名。 */
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return AopUtils.getMostSpecificMethod(signature.getMethod(), joinPoint.getTarget().getClass());
    }

    /** 根据注解、显式 SpEL、登录人和执行结果构建版本化审计事件。 */
    private AuditEvent buildEvent(
            AuditOperation operation,
            Method method,
            Object[] arguments,
            Object result,
            Throwable error,
            AuditActor actor,
            String auditResult,
            String failureCode,
            long startedNanos
    ) {
        /** 注解声明并去除空项后的纯审计分类路径。 */
        List<String> categoryPath = Arrays.stream(operation.categoryPath())
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .toList();
        /** 注解表达式解析的目标 ID，缺失时使用当前操作者 ID。 */
        String targetId = expression(
                operation.targetId(), method, arguments, result, error, actor
        );
        if (targetId == null) {
            targetId = Long.toString(actor.operatorId());
        }
        return new AuditEvent(
                UUID.randomUUID().toString(),
                SCHEMA_VERSION,
                required(operation.source(), "source"),
                categoryPath,
                required(operation.actionCode(), "actionCode"),
                required(operation.actionName(), "actionName"),
                required(operation.targetType(), "targetType"),
                targetId,
                expression(operation.targetName(), method, arguments, result, error, actor),
                expression(operation.targetCode(), method, arguments, result, error, actor),
                expression(operation.reason(), method, arguments, result, error, actor),
                auditResult,
                failureCode,
                expression(operation.maskedMobile(), method, arguments, result, error, actor),
                actor,
                MDC.get("traceId"),
                OffsetDateTime.now(),
                Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L)
        );
    }

    /** 安全计算注解显式字段；表达式错误不允许影响业务执行结果。 */
    private String expression(
            String value,
            Method method,
            Object[] arguments,
            Object result,
            Throwable error,
            AuditActor actor
    ) {
        try {
            return expressionEvaluator.evaluate(value, method, arguments, result, error, actor);
        } catch (RuntimeException exception) {
            log.warn("Audit expression evaluation failed for {}.{}: {}",
                    method.getDeclaringClass().getSimpleName(), method.getName(),
                    exception.getClass().getSimpleName());
            return null;
        }
    }

    /** 成功事件等待当前事务提交；无事务或切面位于事务外层时立即发布。 */
    private void publishAfterCommit(AuditEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                /** 在业务事务成功提交后发布，回滚时不会产生伪成功记录。 */
                @Override
                public void afterCommit() {
                    publishSafely(event);
                }
            });
            return;
        }
        publishSafely(event);
    }

    /** 发布器故障只写安全日志，不能改变业务返回或事务结果。 */
    private void publishSafely(AuditEvent event) {
        try {
            eventPublisher.publish(event);
        } catch (RuntimeException exception) {
            log.error("Audit event publish failed, eventId={}, actionCode={}",
                    event.eventId(), event.actionCode(), exception);
        }
    }

    /** 校验编译期可信注解中的必填文本，避免产生无业务含义事件。 */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Audit annotation " + field + " must not be blank");
        }
        return value.strip();
    }
}
