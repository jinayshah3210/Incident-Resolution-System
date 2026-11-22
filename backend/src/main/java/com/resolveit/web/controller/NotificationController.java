package com.resolveit.web.controller;

import com.resolveit.domain.user.PortalUser;
import com.resolveit.security.CurrentUser;
import com.resolveit.service.NotificationService;
import com.resolveit.web.dto.NotificationDto;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Bell panel: list, unread count, mark read, remove one, clear all. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) { this.notifications = notifications; }

    @GetMapping
    public List<NotificationDto> list(@CurrentUser PortalUser user, @RequestParam(defaultValue = "newest") String sort,
                                      @RequestParam(defaultValue = "false") boolean unreadFirst) {
        return notifications.list(user, sort, unreadFirst);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@CurrentUser PortalUser user) { return Map.of("count", notifications.unreadCount(user)); }

    @PostMapping("/mark-read")
    public Map<String, Boolean> markRead(@CurrentUser PortalUser user) {
        notifications.markAllRead(user);
        return Map.of("success", true);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@CurrentUser PortalUser user, @PathVariable String id) {
        notifications.delete(user, id);
        return Map.of("success", true, "id", id);
    }

    @DeleteMapping
    public Map<String, Object> clear(@CurrentUser PortalUser user) {
        return Map.of("success", true, "removed", notifications.clearAll(user));
    }
}
