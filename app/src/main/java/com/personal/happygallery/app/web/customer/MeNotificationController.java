package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.customer.dto.NotificationResponse;
import com.personal.happygallery.app.web.customer.dto.UnreadCountResponse;
import jakarta.servlet.http.HttpServletRequest;
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
                                           HttpServletRequest request) {
        Long userId = getUserId(request);
        return notificationQuery.listNotifications(userId, null, page, size).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(HttpServletRequest request) {
        long count = notificationQuery.countUnread(getUserId(request), null);
        return new UnreadCountResponse(count);
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id, HttpServletRequest request) {
        notificationQuery.markAsRead(id, getUserId(request), null);
    }

    @PatchMapping("/read-all")
    public void markAllAsRead(HttpServletRequest request) {
        notificationQuery.markAllAsRead(getUserId(request), null);
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }
}
