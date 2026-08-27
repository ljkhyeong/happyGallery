package com.personal.happygallery.application.event.port.in;

import com.personal.happygallery.domain.event.Event;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface EventAdminUseCase {

    List<Event> listAll();

    Event getForEdit(Long id);

    Event create(CreateCommand command);

    Event update(Long id, UpdateCommand command);

    void delete(Long id, long expectedVersion);

    record CreateCommand(
            String title,
            String summary,
            String content,
            String imageUrl,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean published,
            boolean featured,
            Long couponDefinitionId,
            Set<Long> relatedProductIds
    ) {}

    record UpdateCommand(
            long expectedVersion,
            String title,
            String summary,
            String content,
            String imageUrl,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean published,
            boolean featured,
            Long couponDefinitionId,
            Set<Long> relatedProductIds
    ) {}
}
