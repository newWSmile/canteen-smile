package com.canteen.smile.audit.service;

import com.canteen.smile.audit.model.AuditActor;
import com.canteen.smile.audit.model.AuditEvent;
import com.canteen.smile.audit.model.AuditRecordCommand;
import com.canteen.smile.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 无登录态或系统流程使用的编程式审计记录器行为测试。 */
class AuditRecorderTest {

    /** 验证成功事件完整保留后端已确认操作人、登录方式和设备摘要。 */
    @Test
    void shouldRecordTrustedActorForProgrammaticLogin() {
        List<AuditEvent> events = new ArrayList<>();
        AuditRecorder recorder = new AuditRecorder(events::add, () -> "192.168.0.64");
        AuditActor actor = new AuditActor(
                2L, "TENANT_ACCOUNT", 9L, 12L,
                "login_user", "登录用户", "TENANT_ADMIN"
        );
        AuditRecordCommand command = AuditRecordCommand.builder()
                .source("AUTH")
                .categoryPath("租户端", "认证安全", "登录")
                .actionCode("auth:login:sms")
                .actionName("手机号验证码登录")
                .targetType("TENANT_ACCOUNT")
                .targetId(9L)
                .targetName("登录用户")
                .targetCode("login_user")
                .loginMethod("SMS")
                .deviceSummary("WEB / Chrome")
                .actor(actor)
                .build();

        recorder.recordSuccess(command, System.nanoTime());

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.actor()).isSameAs(actor);
            assertThat(event.result()).isEqualTo("SUCCESS");
            assertThat(event.targetId()).isEqualTo("9");
            assertThat(event.loginMethod()).isEqualTo("SMS");
            assertThat(event.deviceSummary()).isEqualTo("WEB / Chrome");
            assertThat(event.ipAddress()).isEqualTo("192.168.0.64");
            assertThat(event.ipHash()).hasSize(64);
            assertThat(event.durationMs()).isGreaterThanOrEqualTo(0L);
        });
    }

    /** 验证编程式失败记录保留业务拒绝错误码。 */
    @Test
    void shouldRecordBusinessDenialWithoutThrowing() {
        List<AuditEvent> events = new ArrayList<>();
        AuditRecorder recorder = new AuditRecorder(events::add);
        AuditRecordCommand command = AuditRecordCommand.builder()
                .source("AUTH")
                .categoryPath("租户端", "认证安全", "登录")
                .actionCode("auth:login:password")
                .actionName("用户名密码登录")
                .targetType("LOGIN_IDENTIFIER")
                .targetId("login_user")
                .actor(AuditActor.anonymous("TENANT_ADMIN"))
                .build();

        recorder.recordFailure(
                command,
                new BusinessException("AUTH_401", "用户名或密码错误", 401),
                System.nanoTime()
        );

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.actor().operatorType()).isEqualTo("ANONYMOUS");
            assertThat(event.result()).isEqualTo("DENIED");
            assertThat(event.failureReasonCode()).isEqualTo("AUTH_401");
        });
    }
}
