package com.resolveit.notify;

/** Adapter target: the minimal mail-sending contract EmailChannel adapts notifications to. */
public interface MailGateway {
    void send(String to, String subject, String body);
}
