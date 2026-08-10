package com.canteen.smile.modules.outbox.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** XXL-JOB 执行器 Spring 生命周期配置测试。 */
class OutboxConfigurationTest {

    /** 执行器自行实现 SmartInitializingSingleton，禁止再声明 initMethod 导致重复启动。 */
    @Test
    void shouldDelegateStartupToXxlJobSpringExecutorLifecycle() throws NoSuchMethodException {
        Method factoryMethod = OutboxConfiguration.class.getMethod(
                "xxlJobExecutor", OutboxXxlJobProperties.class
        );

        Bean bean = factoryMethod.getAnnotation(Bean.class);

        assertThat(bean).isNotNull();
        assertThat(bean.initMethod()).isEmpty();
    }
}
