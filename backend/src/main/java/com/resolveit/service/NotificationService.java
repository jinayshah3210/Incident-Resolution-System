package com.resolveit.service;

import com.resolveit.domain.notification.Notification;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.exception.UnauthorizedOperationException;
import com.resolveit.notify.NotificationChannel;
import com.resolveit.repository.NotificationRepository;
import com.resolveit.repository.UserRepository;
import com.resolveit.web.dto.NotificationDto;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sends each notification through every enabled channel and serves the bell panel. */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final List<NotificationChannel> channels;
    private final NotificationRepository repository;
    private final UserRepository users;

    public NotificationService(List<NotificationChannel> channels, NotificationRepository repository, UserRepository users) {
        this.channels = channels;
        this.repository = repository;
        this.users = users;
    }

    /** Polymorphic delivery: the service never knows which concrete channels exist. */
    public void send(Notification notification) {
        PortalUser recipient = users.findById(notification.getUserId()).orElse(null);
        for (NotificationChannel channel : channels) {
            if (!channel.isEnabled()) continue;
            try {
                channel.deliver(notification, recipient);
            } catch (RuntimeException failure) {
                log.warn("Channel {} failed for {}: {}", channel.name(), notification.getUserId(), failure.getMessage());
            }
        }
    }

    public List<String> channelNames() { return channels.stream().map(NotificationChannel::name).toList(); }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(PortalUser user, String sort, boolean unreadFirst) {
        Comparator<Notification> order = Comparator.comparing(Notification::getCreatedAt);
        if (!"oldest".equalsIgnoreCase(sort)) order = order.reversed();
        if (unreadFirst) order = Comparator.comparing(Notification::isRead).thenComparing(order);
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().sorted(order).map(NotificationService::dto).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(PortalUser user) { return repository.countByUserIdAndReadFalse(user.getId()); }

    @Transactional
    public void markAllRead(PortalUser user) {
        repository.findByUserIdOrderByCreatedAtDesc(user.getId()).forEach(Notification::markRead);
    }

    @Transactional
    public void delete(PortalUser user, String id) {
        Notification n = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        if (!n.belongsTo(user.getId())) throw new UnauthorizedOperationException("You can only remove your own notifications.");
        repository.delete(n);
    }

    @Transactional
    public long clearAll(PortalUser user) {
        long count = repository.countByUserId(user.getId());
        repository.deleteByUserId(user.getId());
        return count;
    }

    public static NotificationDto dto(Notification n) {
        return new NotificationDto(n.getId(), n.getUserId(), n.getIncidentId(), n.getType().name(), n.getTitle(),
                n.getMessage(), n.getCreatedAt(), n.isRead());
    }
}
