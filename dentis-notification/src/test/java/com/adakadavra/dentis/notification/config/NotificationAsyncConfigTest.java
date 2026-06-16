package com.adakadavra.dentis.notification.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("NotificationAsyncConfig")
class NotificationAsyncConfigTest {

    private final NotificationAsyncConfig config = new NotificationAsyncConfig();

    @Test
    @DisplayName("should create executor with notification- thread name prefix")
    void shouldCreateExecutorWithNotificationThreadPrefix() {
        Executor executor = config.notificationExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        assertThat(pool.getThreadNamePrefix()).isEqualTo("notification-");
    }

    @Test
    @DisplayName("should create executor with bounded queue capacity")
    void shouldCreateExecutorWithBoundedQueueCapacity() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) config.notificationExecutor();

        assertThat(pool.getCorePoolSize()).isGreaterThanOrEqualTo(1);
        assertThat(pool.getMaxPoolSize()).isGreaterThanOrEqualTo(pool.getCorePoolSize());
    }

    @Test
    @DisplayName("should provide an AsyncUncaughtExceptionHandler")
    void shouldProvideAsyncUncaughtExceptionHandler() {
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();

        assertThat(handler).isNotNull();
    }

    @Test
    @DisplayName("should not throw when AsyncUncaughtExceptionHandler handles an exception")
    void shouldNotThrowWhenHandlerReceivesException() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");

        assertThatCode(() -> handler.handleUncaughtException(
                new RuntimeException("async failure"), method))
                .doesNotThrowAnyException();
    }
}
