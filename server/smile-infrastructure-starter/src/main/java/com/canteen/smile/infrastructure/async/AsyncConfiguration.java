package com.canteen.smile.infrastructure.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/** 统一异步执行器，禁止业务代码直接创建线程池或使用默认公共池。 */
@Configuration
@EnableConfigurationProperties(AsyncProperties.class)
public class AsyncConfiguration implements AsyncConfigurer {

    /** 当前类日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

    /** 业务线程池配置。 */
    private final AsyncProperties properties;

    /**
     * 注入线程池配置。
     *
     * @param properties 线程池配置
     */
    public AsyncConfiguration(AsyncProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建有界的普通异步业务线程池。
     * 文件、消息和第三方调用等任务应在出现真实需求时建立隔离线程池。
     *
     * @return 普通异步任务执行器
     */
    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.corePoolSize());
        executor.setMaxPoolSize(properties.maxPoolSize());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setKeepAliveSeconds(properties.keepAliveSeconds());
        executor.setThreadNamePrefix("app-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(properties.taskTimeoutSeconds());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    /** @return Spring `@Async` 默认执行器 */
    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor();
    }

    /**
     * 记录无返回值异步方法抛出的异常。
     *
     * @return 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new LoggingAsyncExceptionHandler();
    }

    /** 统一记录异步异常，禁止静默吞掉。 */
    private static final class LoggingAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        /**
         * 记录异步异常及方法信息，不记录可能包含敏感数据的参数。
         *
         * @param throwable 异步异常
         * @param method 发生异常的方法
         * @param parameters 方法参数，仅用于满足接口，不写入日志
         */
        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... parameters) {
            log.error("Unhandled async error in {}.{}", method.getDeclaringClass().getSimpleName(), method.getName(), throwable);
        }
    }
}
