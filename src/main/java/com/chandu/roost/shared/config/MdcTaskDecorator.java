package com.chandu.roost.shared.config;

import org.slf4j.MDC;
import org.springframework.boot.task.SimpleAsyncTaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Copies the logging MDC from the submitting thread onto the async thread so
 * correlation ids survive {@code @Async} hops. Without this, a probe dispatched
 * off the request thread logs with an empty {@code requestId}.
 *
 * <p>The decorator snapshots the MDC at submit time, installs it around the
 * task, and restores the worker thread's previous MDC afterwards (important for
 * reused platform/virtual threads).
 */
@Configuration
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> submitterContext = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (submitterContext != null) {
                MDC.setContextMap(submitterContext);
            } else {
                MDC.clear();
            }
            try {
                runnable.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    /** Wires this decorator into Spring Boot's auto-configured async executor. */
    @Bean
    SimpleAsyncTaskExecutorCustomizer mdcAsyncCustomizer() {
        return executor -> executor.setTaskDecorator(this);
    }
}
