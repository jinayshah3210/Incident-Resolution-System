package com.resolveit.repository;

import com.resolveit.domain.notification.EmailOutboxEntry;
import com.resolveit.domain.notification.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository pattern: simulated email outbox. */
public interface EmailOutboxRepository extends JpaRepository<EmailOutboxEntry, Long> {
    long countByStatus(EmailStatus status);
}
