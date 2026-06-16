package com.adakadavra.dentis.notification.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("NotificationAsyncConfig")
class NotificationAsyncConfigTest {

    private final NotificationAsyncConfig config = new NotificationAsyncConfig();

    @Test
    @DisplayName("should create a SimpleAsyncTaskExecutor backed by virtual threads")
    void shouldCreateVirtualThreadExecutor() {
        Executor executor = config.notificationExecutor();

        assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);
    }

    @Test
    @DisplayName("should name threads with notification- prefix for tracing")
    void shouldNameThreadsWithNotificationPrefix() {
        SimpleAsyncTaskExecutor executor = (SimpleAsyncTaskExecutor) config.notificationExecutor();

        assertThat(executor.getThreadNamePrefix()).isEqualTo("notification-");
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
