package com.personal.happygallery.application.event.port.out;

import com.personal.happygallery.domain.event.Event;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventReaderPort {

    Optional<Event> findByIdWithRelatedProducts(Long id);

    Optional<Event> findPublicById(Long id, LocalDateTime now);

    List<Event> findPublicEvents(LocalDateTime now);

    List<Event> findAllForAdmin();
}
