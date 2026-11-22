package com.resolveit.repository;

import com.resolveit.domain.notification.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository pattern: per-user notifications. */
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    long countByUserIdAndReadFalse(String userId);
    long countByUserId(String userId);
    void deleteByUserId(String userId);
}
