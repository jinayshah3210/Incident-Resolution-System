package com.resolveit.scheduler;

import com.resolveit.seed.DemoDataSeeder;
import com.resolveit.service.SlaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every 30 s (5 s in the demo profile): SLA warnings, violations and escalations for all open incidents. */
@Component
public class SlaMonitorJob {
    private static final Logger log = LoggerFactory.getLogger(SlaMonitorJob.class);
    private final SlaService sla;
    private final DemoDataSeeder seeder;

    public SlaMonitorJob(SlaService sla, DemoDataSeeder seeder) {
        this.sla = sla;
        this.seeder = seeder;
    }

    @Scheduled(fixedDelayString = "${resolveit.sla.monitor-ms:30000}", initialDelay = 10000)
    public void run() {
        if (seeder.isRunning()) return;
        try {
            sla.monitorOpenIncidents();
        } catch (RuntimeException failure) {
            log.warn("SLA monitor pass failed: {}", failure.getMessage());
        }
    }
}
