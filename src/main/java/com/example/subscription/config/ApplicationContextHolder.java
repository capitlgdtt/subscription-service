package com.example.subscription.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Static bridge to the Spring {@link ApplicationContext}.
 *
 * <p>onno entities are plain POJOs instantiated by the framework and by Spring Data JDBC —
 * not Spring beans — so they cannot receive dependencies through constructor injection. Yet
 * a document's {@code beforeWrite} hook occasionally needs to read master data (e.g. a line's
 * price and period length from the {@code Tariff} catalog) that lives in a repository. This
 * holder captures the context once at startup and lets those POJOs look repositories up on
 * demand.</p>
 *
 * <p><b>Use sparingly.</b> It is a deliberate escape hatch from DI, not a pattern to copy
 * everywhere: pull a repository this way only when the caller genuinely cannot be a Spring
 * bean and genuinely needs one — otherwise inject normally.</p>
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        context = ctx;
    }

    /**
     * Resolve a bean by type. Fails fast if called before the context is ready — an early
     * call would otherwise surface later as a confusing {@code NullPointerException}.
     */
    public static <T> T getBean(Class<T> type) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext is not initialized yet");
        }
        return context.getBean(type);
    }
}