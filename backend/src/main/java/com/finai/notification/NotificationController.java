package com.finai.notification;

import com.finai.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Budget alerts and system messages for the caller")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List the caller's notifications, newest first")
    public PageResponse<NotificationService.NotificationView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.list(page, size);
    }

    @GetMapping("/unread")
    @Operation(summary = "List the caller's unread notifications")
    public List<NotificationService.NotificationView> unread() {
        return notificationService.unread();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Return how many notifications are unread")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.unreadCount());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public NotificationService.NotificationView markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark every notification as read")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
