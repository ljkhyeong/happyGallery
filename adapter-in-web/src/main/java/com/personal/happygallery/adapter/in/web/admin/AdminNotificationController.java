package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.FailedNotificationResponse;
import com.personal.happygallery.application.notification.port.in.NotificationFailureAdminUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class AdminNotificationController {

    private final NotificationFailureAdminUseCase notificationFailureAdminUseCase;

    public AdminNotificationController(NotificationFailureAdminUseCase notificationFailureAdminUseCase) {
        this.notificationFailureAdminUseCase = notificationFailureAdminUseCase;
    }

    @GetMapping("/failed")
    public List<FailedNotificationResponse> listFailed() {
        return notificationFailureAdminUseCase.listFailed().stream()
                .map(FailedNotificationResponse::from)
                .toList();
    }

    @PostMapping("/{outboxId}/retry")
    public FailedNotificationResponse retry(@PathVariable Long outboxId) {
        return FailedNotificationResponse.from(notificationFailureAdminUseCase.retry(outboxId));
    }
}
