package com.resolveit.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the SLA monitor and assignment retry jobs (disabled in automated tests). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "resolveit.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig { }
