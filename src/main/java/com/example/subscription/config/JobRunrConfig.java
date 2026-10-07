package com.example.subscription.config;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Wires JobRunr's storage against the application's own {@link DataSource}.
 *
 * <p>onno uses JobRunr as its background-job engine ({@code @ScheduledJob} +
 * {@code BackgroundTask}), and JobRunr needs a {@code StorageProvider} to persist jobs,
 * recurring schedules, and their metadata. The JobRunr 7 Spring Boot starter normally
 * auto-detects a {@code DataSource}, but when the onno starter sits between them the
 * auto-configuration does not fire and the context fails to start with
 * "No qualifying bean of type StorageProvider". Defining the bean explicitly is the
 * documented escape hatch: the framework reuses whatever {@code DataSource} the application
 * already configured, so JobRunr's tables live in the same database as the domain data.</p>
 *
 * <p>JobRunr runs its own schema migrations against this provider on first startup; no manual
 * DDL is required.</p>
 */
@Configuration
public class JobRunrConfig {

    @Bean
    public StorageProvider storageProvider(DataSource dataSource, JobMapper jobMapper) {
        var provider = SqlStorageProviderFactory.using(dataSource);
        provider.setJobMapper(jobMapper);
        return provider;
    }
}