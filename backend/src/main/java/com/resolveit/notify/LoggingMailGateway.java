package com.resolveit.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Adapter: default gateway that only logs; replace with an SMTP implementation to send real mail. */
@Component
public class LoggingMailGateway implements MailGateway {
    private static final Logger log = LoggerFactory.getLogger(LoggingMailGateway.class);

    @Override public void send(String to, String subject, String body) {
        log.info("[mail] to={} subject={}", to, subject);
    }
}
