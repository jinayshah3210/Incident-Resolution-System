package com.resolveit.scheduler;

import com.resolveit.seed.DemoDataSeeder;
import com.resolveit.service.AssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every 60 s: retries unassigned incidents, P1 first (PriorityQueue inside AssignmentService). */
@Component
public class AssignmentRetryJob {
    private static final Logger log = LoggerFactory.getLogger(AssignmentRetryJob.class);
    private final AssignmentService assignment;
    private final DemoDataSeeder seeder;

    public AssignmentRetryJob(AssignmentService assignment, DemoDataSeeder seeder) {
        this.assignment = assignment;
        this.seeder = seeder;
    }

    @Scheduled(fixedDelayString = "${resolveit.assignment.retry-ms:60000}", initialDelay = 15000)
    public void run() {
        if (seeder.isRunning()) return;
        try {
            assignment.retryUnassigned();
        } catch (RuntimeException failure) {
            log.warn("Assignment retry failed: {}", failure.getMessage());
        }
    }
}
