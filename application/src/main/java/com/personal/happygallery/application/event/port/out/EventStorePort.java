package com.personal.happygallery.application.event.port.out;

import com.personal.happygallery.domain.event.Event;

public interface EventStorePort {

    Event save(Event event);

    void deleteById(Long id);
}
