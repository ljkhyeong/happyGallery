package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.adapter.in.web.customer.dto.NotificationResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.UnreadCountResponse;
import com.personal.happygallery.adapter.in.web.resolver.CustomerUserId;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/notifications")
public class MeNotificationController {

    private final NotificationQueryUseCase notificationQuery;

    public MeNotificationController(NotificationQueryUseCase notificationQuery) {
        this.notificationQuery = notificationQuery;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @CustomerUserId Long userId) {
        return notificationQuery.listNotifications(userId, null, page, size).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@CustomerUserId Long userId) {
        long count = notificationQuery.countUnread(userId, null);
        return new UnreadCountResponse(count);
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id, @CustomerUserId Long userId) {
        notificationQuery.markAsRead(id, userId, null);
    }

    @PatchMapping("/read-all")
    public void markAllAsRead(@CustomerUserId Long userId) {
        notificationQuery.markAllAsRead(userId, null);
    }
}
