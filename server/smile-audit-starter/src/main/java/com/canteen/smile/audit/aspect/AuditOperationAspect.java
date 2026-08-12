package com.canteen.smile.audit.aspect;

import com.canteen.smile.audit.annotation.AuditOperation;
import com.canteen.smile.audit.expression.AuditExpressionEvaluator;
import com.canteen.smile.audit.model.AuditActor;
import com.canteen.smile.audit.model.AuditRecordCommand;
import com.canteen.smile.audit.service.AuditRecorder;
import com.canteen.smile.audit.spi.AuditActorResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/** 拦截审计注解并在业务提交后发布不影响主事务的异步事件。 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class AuditOperationAspect {

    /** 当前类安全日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(AuditOperationAspect.class);

    /** 当前服务的登录人解析器。 */
    private final AuditActorResolver actorResolver;

    /** 注解与编程式入口共用的审计事件记录器。 */
    private final AuditRecorder auditRecorder;

    /** 注解字段表达式求值器。 */
    private final AuditExpressionEvaluator expressionEvaluator;

    /**
     * 创建审计切面。
     *
     * @param actorResolver 登录人解析器
     * @param auditRecorder 审计事件记录器
     * @param expressionEvaluator 显式字段表达式求值器
     */
    public AuditOperationAspect(
            AuditActorResolver actorResolver,
            AuditRecorder auditRecorder,
            AuditExpressionEvaluator expressionEvaluator
    ) {
        this.actorResolver = actorResolver;
        this.auditRecorder = auditRecorder;
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
            AuditRecordCommand command = buildCommand(
                    operation, method, arguments, result, null, actor
            );
            auditRecorder.recordSuccess(command, startedNanos);
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
            AuditRecordCommand command = buildCommand(
                    operation, method, arguments, null, error, actor
            );
            auditRecorder.recordFailure(command, error, startedNanos);
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

    /** 根据注解、显式 SpEL 和当前登录人构建编程式审计声明。 */
    private AuditRecordCommand buildCommand(
            AuditOperation operation,
            Method method,
            Object[] arguments,
            Object result,
            Throwable error,
            AuditActor actor
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
        return new AuditRecordCommand(
                operation.source(),
                categoryPath,
                operation.actionCode(),
                operation.actionName(),
                operation.targetType(),
                targetId,
                expression(operation.targetName(), method, arguments, result, error, actor),
                expression(operation.targetCode(), method, arguments, result, error, actor),
                expression(operation.reason(), method, arguments, result, error, actor),
                expression(operation.maskedMobile(), method, arguments, result, error, actor),
                expression(operation.loginMethod(), method, arguments, result, error, actor),
                expression(operation.deviceSummary(), method, arguments, result, error, actor),
                actor
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

}
