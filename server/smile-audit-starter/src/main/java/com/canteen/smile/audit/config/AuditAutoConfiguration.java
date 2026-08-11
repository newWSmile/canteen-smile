package com.canteen.smile.audit.config;

import com.canteen.smile.audit.aspect.AuditOperationAspect;
import com.canteen.smile.audit.expression.AuditExpressionEvaluator;
import com.canteen.smile.audit.model.AuditActor;
import com.canteen.smile.audit.spi.AuditActorResolver;
import com.canteen.smile.audit.spi.AuditEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/** 装配可由业务服务和未来 MQ 实现替换的通用审计基础能力。 */
@AutoConfiguration
public class AuditAutoConfiguration {

    /** @return 只计算注解显式字段的表达式求值器 */
    @Bean
    @ConditionalOnMissingBean
    public AuditExpressionEvaluator auditExpressionEvaluator() {
        return new AuditExpressionEvaluator();
    }

    /** @return 服务未提供登录人解析器时使用的明确系统主体 */
    @Bean
    @ConditionalOnMissingBean(AuditActorResolver.class)
    public AuditActorResolver systemAuditActorResolver() {
        return AuditActor::system;
    }

    /** @param applicationEventPublisher Spring 本地事件总线 @return 当前本地事件发布器 */
    @Bean
    @ConditionalOnMissingBean(AuditEventPublisher.class)
    public AuditEventPublisher localAuditEventPublisher(
            ApplicationEventPublisher applicationEventPublisher
    ) {
        return applicationEventPublisher::publishEvent;
    }

    /** @return 审计 Service 方法的统一切面 */
    @Bean
    public AuditOperationAspect auditOperationAspect(
            AuditActorResolver actorResolver,
            AuditEventPublisher eventPublisher,
            AuditExpressionEvaluator expressionEvaluator
    ) {
        return new AuditOperationAspect(actorResolver, eventPublisher, expressionEvaluator);
    }
}
