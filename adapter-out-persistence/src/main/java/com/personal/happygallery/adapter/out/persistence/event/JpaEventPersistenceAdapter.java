package com.personal.happygallery.adapter.out.persistence.event;

import com.personal.happygallery.application.event.port.out.EventStorePort;
import com.personal.happygallery.domain.event.Event;
import org.springframework.stereotype.Repository;

@Repository
class JpaEventPersistenceAdapter implements EventStorePort {

    private final EventRepository repository;

    JpaEventPersistenceAdapter(EventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Event save(Event event) {
        return repository.save(event);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
