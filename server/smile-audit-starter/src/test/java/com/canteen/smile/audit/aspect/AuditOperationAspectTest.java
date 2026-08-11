package com.canteen.smile.audit.aspect;

import com.canteen.smile.audit.annotation.AuditOperation;
import com.canteen.smile.audit.expression.AuditExpressionEvaluator;
import com.canteen.smile.audit.model.AuditActor;
import com.canteen.smile.audit.model.AuditEvent;
import com.canteen.smile.common.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 通用审计切面登录人快照、表达式和业务隔离行为测试。 */
class AuditOperationAspectTest {

    /** 验证登录人在业务线程解析且随成功事件固化到异步载荷。 */
    @Test
    void shouldCaptureActorBeforePublishingSuccessEvent() throws Throwable {
        AuditActor actor = new AuditActor(
                2L, "TENANT_ACCOUNT", 7L, 11L,
                "test_user", "测试用户", "TENANT_ADMIN"
        );
        List<AuditEvent> events = new ArrayList<>();
        AuditOperationAspect aspect = new AuditOperationAspect(
                () -> actor,
                events::add,
                new AuditExpressionEvaluator()
        );
        SampleService service = new SampleService();
        Method method = SampleService.class.getMethod("bind", String.class);
        ProceedingJoinPoint joinPoint = joinPoint(service, method, "138****8000");
        SampleResult result = new SampleResult("138****8000");
        when(joinPoint.proceed()).thenReturn(result);

        Object actual = aspect.around(joinPoint, method.getAnnotation(AuditOperation.class));

        assertThat(actual).isSameAs(result);
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.actor()).isSameAs(actor);
            assertThat(event.actionName()).isEqualTo("绑定手机号");
            assertThat(event.targetId()).isEqualTo("7");
            assertThat(event.maskedMobile()).isEqualTo("138****8000");
            assertThat(event.categoryPath()).containsExactly("租户端", "账号安全", "手机号凭证");
        });
    }

    /** 验证审计发布器故障不会改变已经成功的业务返回。 */
    @Test
    void shouldNotFailBusinessWhenPublisherFails() throws Throwable {
        AuditOperationAspect aspect = new AuditOperationAspect(
                AuditActor::system,
                event -> { throw new IllegalStateException("audit unavailable"); },
                new AuditExpressionEvaluator()
        );
        SampleService service = new SampleService();
        Method method = SampleService.class.getMethod("bind", String.class);
        ProceedingJoinPoint joinPoint = joinPoint(service, method, "138****8000");
        SampleResult result = new SampleResult("138****8000");
        when(joinPoint.proceed()).thenReturn(result);

        assertThat(aspect.around(joinPoint, method.getAnnotation(AuditOperation.class)))
                .isSameAs(result);
    }

    /** 验证失败审计不能替换原始业务错误码和异常实例。 */
    @Test
    void shouldPreserveOriginalBusinessException() throws Throwable {
        List<AuditEvent> events = new ArrayList<>();
        AuditOperationAspect aspect = new AuditOperationAspect(
                AuditActor::system,
                events::add,
                new AuditExpressionEvaluator()
        );
        SampleService service = new SampleService();
        Method method = SampleService.class.getMethod("bind", String.class);
        ProceedingJoinPoint joinPoint = joinPoint(service, method, "138****8000");
        BusinessException original = new BusinessException("AUTH_TEST", "测试拒绝", 403);
        when(joinPoint.proceed()).thenThrow(original);

        assertThatThrownBy(() -> aspect.around(
                joinPoint, method.getAnnotation(AuditOperation.class)
        )).isSameAs(original);
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.result()).isEqualTo("DENIED");
            assertThat(event.failureReasonCode()).isEqualTo("AUTH_TEST");
        });
    }

    /** 验证登录事件使用服务端已验证上下文固化主体和登录信息。 */
    @Test
    void shouldCaptureLoginActorFromTrustedContext() throws Throwable {
        List<AuditEvent> events = new ArrayList<>();
        AuditOperationAspect aspect = new AuditOperationAspect(
                AuditActor::system, events::add, new AuditExpressionEvaluator()
        );
        LoginService service = new LoginService();
        Method method = LoginService.class.getMethod("login", LoginContext.class);
        LoginContext context = new LoginContext(
                2L, 9L, 12L, "login_user", "登录用户", "WEB", "Chrome"
        );
        ProceedingJoinPoint joinPoint = joinPoint(service, method, context);
        when(joinPoint.proceed()).thenReturn("OK");

        aspect.around(joinPoint, method.getAnnotation(AuditOperation.class));

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.actor().operatorType()).isEqualTo("TENANT_ACCOUNT");
            assertThat(event.actor().operatorId()).isEqualTo(9L);
            assertThat(event.actor().tenantId()).isEqualTo(2L);
            assertThat(event.actor().organizationId()).isEqualTo(12L);
            assertThat(event.loginMethod()).isEqualTo("SMS");
            assertThat(event.deviceSummary()).isEqualTo("WEB / Chrome");
            assertThat(event.durationMs()).isGreaterThanOrEqualTo(0L);
        });
    }

    /** 验证无登录会话的敏感流程可从服务端可信上下文固化真实操作者。 */
    @Test
    void shouldOverrideActorFromTrustedServerContext() throws Throwable {
        List<AuditEvent> events = new ArrayList<>();
        AuditOperationAspect aspect = new AuditOperationAspect(
                AuditActor::system, events::add, new AuditExpressionEvaluator()
        );
        PasswordResetService service = new PasswordResetService();
        Method method = PasswordResetService.class.getMethod(
                "reset", ResetContext.class
        );
        ResetContext context = new ResetContext(
                2L, 9L, 12L, "reset_user", "找回用户"
        );
        ProceedingJoinPoint joinPoint = joinPoint(service, method, context);
        when(joinPoint.proceed()).thenReturn("OK");

        aspect.around(joinPoint, method.getAnnotation(AuditOperation.class));

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.actor().operatorType()).isEqualTo("TENANT_ACCOUNT");
            assertThat(event.actor().operatorId()).isEqualTo(9L);
            assertThat(event.actor().tenantId()).isEqualTo(2L);
            assertThat(event.actor().organizationId()).isEqualTo(12L);
            assertThat(event.actor().username()).isEqualTo("reset_user");
            assertThat(event.targetId()).isEqualTo("9");
        });
    }

    /** 创建暴露具体方法、参数和目标对象的连接点替身。 */
    private ProceedingJoinPoint joinPoint(Object target, Method method, Object... arguments) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getArgs()).thenReturn(arguments);
        return joinPoint;
    }

    /** 仅用于验证注解表达式的样例 Service。 */
    static class SampleService {

        /** @return 样例脱敏绑定结果 */
        @AuditOperation(
                source = "AUTH",
                categoryPath = {"租户端", "账号安全", "手机号凭证"},
                actionCode = "auth:mobile:bind",
                actionName = "绑定手机号",
                targetType = "TENANT_ACCOUNT",
                targetId = "#actor.operatorId",
                maskedMobile = "#result?.maskedMobile"
        )
        public SampleResult bind(String maskedMobile) {
            return new SampleResult(maskedMobile);
        }
    }

    /** 仅用于验证登录后主体解析的样例 Service。 */
    static class LoginService {

        /** @return 样例登录结果 */
        @AuditOperation(
                source = "AUTH",
                categoryPath = {"租户端", "认证安全", "登录"},
                actionCode = "auth:login:sms",
                actionName = "手机号验证码登录",
                targetType = "TENANT_ACCOUNT",
                targetId = "#context.accountId",
                loginMethod = "SMS",
                deviceSummary = "#context.deviceType + ' / ' + #context.deviceName",
                actorType = "TENANT_ACCOUNT",
                actorId = "#context.accountId",
                actorTenantId = "#context.tenantId",
                actorOrganizationId = "#context.organizationId",
                actorUsername = "#context.username",
                actorDisplayName = "#context.displayName",
                actorAppCode = "TENANT_ADMIN"
        )
        public String login(LoginContext context) {
            return "OK";
        }
    }

    /** 仅用于验证服务端可信主体覆盖的样例 Service。 */
    static class PasswordResetService {

        /** @return 样例密码重置结果 */
        @AuditOperation(
                source = "AUTH",
                categoryPath = {"租户端", "账号安全", "密码凭证"},
                actionCode = "auth:password:reset:sms",
                actionName = "手机号验证码自助重置密码",
                targetType = "TENANT_ACCOUNT",
                targetId = "#context.accountId",
                actorType = "TENANT_ACCOUNT",
                actorId = "#context.accountId",
                actorTenantId = "#context.tenantId",
                actorOrganizationId = "#context.organizationId",
                actorUsername = "#context.username",
                actorDisplayName = "#context.displayName"
        )
        public String reset(ResetContext context) {
            return "OK";
        }
    }

    /** @param maskedMobile 样例脱敏手机号 */
    record SampleResult(String maskedMobile) {
    }

    /** @param tenantId 租户 ID @param accountId 账号 ID @param organizationId 机构 ID @param username 用户名 @param displayName 显示名称 @param deviceType 设备类型 @param deviceName 设备名称 */
    record LoginContext(
            long tenantId,
            long accountId,
            long organizationId,
            String username,
            String displayName,
            String deviceType,
            String deviceName
    ) {
    }

    /** @param tenantId 租户 ID @param accountId 账号 ID @param organizationId 机构 ID @param username 用户名 @param displayName 显示名称 */
    record ResetContext(
            long tenantId,
            long accountId,
            long organizationId,
            String username,
            String displayName
    ) {
    }
}
