package com.personal.happygallery.application.event.port.in;

import com.personal.happygallery.domain.event.Event;
import java.util.List;

public interface EventQueryUseCase {

    List<Event> listPublicEvents();

    Event getPublicEvent(Long id);
}
