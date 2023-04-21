package com.example.jobrunrautocommitissuepoc;

import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = JobRunrAutoCommitIssuePocTests.Initializer.class)
class JobRunrAutoCommitIssuePocTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrAutoCommitIssuePocTests.class);

    @Container
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer()
            .withDatabaseName("integration-tests-db")
            .withUsername("sa")
            .withPassword("sa");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private StorageProvider storageProvider;

    @Autowired LongRunningJob longRunningJob;

    @Autowired BackgroundJobServer backgroundJobServer;

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void contextLoads() throws InterruptedException {

        JobId job1 = jobScheduler.enqueue(() -> longRunningJob.run(JobContext.Null));
        LOGGER.info("Job {} enqueued.", job1.asUUID());

        JobId job2 = jobScheduler.enqueue(() -> longRunningJob.run(JobContext.Null));
        LOGGER.info("Job {} enqueued.", job2.asUUID());

        while (backgroundJobServer.isRunning()) {
            Thread.sleep(1000);
            storageProvider.deletePermanently(job1.asUUID());
            LOGGER.info("Job {} deleted", job2.asUUID());
        }

        fail("Background job server should have stopped");
    }

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }
}
