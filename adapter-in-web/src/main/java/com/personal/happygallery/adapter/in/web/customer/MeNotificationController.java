package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.adapter.in.web.customer.dto.NotificationResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.UnreadCountResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @Operation(operationId = "listMyNotifications")
    public List<NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(defaultValue = "false") boolean unreadOnly,
                                           @AuthenticationPrincipal CustomerPrincipal customer) {
        return notificationQuery.listNotifications(customer.userId(), null, page, size, unreadOnly).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @GetMapping("/unread-count")
    @Operation(operationId = "getMyUnreadNotificationCount")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal CustomerPrincipal customer) {
        long count = notificationQuery.countUnread(customer.userId(), null);
        return new UnreadCountResponse(count);
    }

    @PatchMapping("/{id}/read")
    @Operation(operationId = "markMyNotificationAsRead")
    public void markAsRead(@PathVariable Long id,
                           @AuthenticationPrincipal CustomerPrincipal customer) {
        notificationQuery.markAsRead(id, customer.userId(), null);
    }

    @PatchMapping("/read-all")
    @Operation(operationId = "markAllMyNotificationsAsRead")
    public void markAllAsRead(@AuthenticationPrincipal CustomerPrincipal customer) {
        notificationQuery.markAllAsRead(customer.userId(), null);
    }
}
