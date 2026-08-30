package com.personal.happygallery.application.notification.port.in;

import com.personal.happygallery.domain.notification.NotificationOutbox;
import java.util.List;

public interface NotificationFailureAdminUseCase {

    List<NotificationOutbox> listFailed();

    NotificationOutbox retry(Long outboxId);
}
