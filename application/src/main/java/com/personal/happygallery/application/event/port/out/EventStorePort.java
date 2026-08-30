package com.personal.happygallery.application.event.port.out;

import com.personal.happygallery.domain.event.Event;

public interface EventStorePort {

    <S extends Event> S save(S event);

    void deleteById(Long id);
}
