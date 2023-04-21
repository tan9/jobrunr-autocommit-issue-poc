package com.example.jobrunrautocommitissuepoc;

import org.jobrunr.jobs.context.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LongRunningJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(LongRunningJob.class);

    public void run(JobContext jobContext) throws InterruptedException {
        int count = 0;
        while (true) {
            LOGGER.info("Running job {}: {}", jobContext.getJobId(), count);
            count++;

            Thread.sleep(10000);
        }
    }
}
