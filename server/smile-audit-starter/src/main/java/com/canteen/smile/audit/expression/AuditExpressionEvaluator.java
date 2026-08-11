package com.canteen.smile.audit.expression;

import com.canteen.smile.audit.model.AuditActor;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;

/** 只计算注解显式声明的字段表达式，禁止自动序列化全部方法参数。 */
public class AuditExpressionEvaluator {

    /** SpEL 解析器。 */
    private final ExpressionParser parser = new SpelExpressionParser();

    /** Java 方法参数名称发现器。 */
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();

    /**
     * 计算固定文本或以井号开头的 SpEL 表达式。
     *
     * @param value 注解字段值
     * @param method 当前公共 Service 方法
     * @param arguments 方法参数
     * @param result 可选返回值
     * @param error 可选异常
     * @param actor 业务线程提前解析的登录人
     * @return 去除首尾空白的可选文本
     */
    public String evaluate(
            String value,
            Method method,
            Object[] arguments,
            Object result,
            Throwable error,
            AuditActor actor
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        /** 注解中去除首尾空白的固定文本或表达式。 */
        String candidate = value.strip();
        if (!candidate.startsWith("#")) {
            return candidate;
        }
        /** 包含参数名、返回值、异常和登录人的受控求值上下文。 */
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null, method, arguments, parameterNameDiscoverer
        );
        context.setVariable("result", result);
        context.setVariable("error", error);
        context.setVariable("actor", actor);
        /** 表达式计算出的原始值。 */
        Object evaluated = parser.parseExpression(candidate).getValue(context);
        if (evaluated == null || String.valueOf(evaluated).isBlank()) {
            return null;
        }
        return String.valueOf(evaluated).strip();
    }
}
