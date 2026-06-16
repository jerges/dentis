package com.adakadavra.dentis.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
public class NotificationAsyncConfig implements AsyncConfigurer {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("notification-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("Unhandled async exception in {}.{}",
                        method.getDeclaringClass().getSimpleName(), method.getName(), ex);
    }
}
