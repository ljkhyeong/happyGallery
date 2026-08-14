package com.personal.happygallery.application.event;

import com.personal.happygallery.application.event.port.in.EventAdminUseCase;
import com.personal.happygallery.application.event.port.in.EventQueryUseCase;
import com.personal.happygallery.application.event.port.out.EventReaderPort;
import com.personal.happygallery.application.event.port.out.EventStorePort;
import com.personal.happygallery.application.media.ImageMediaReferenceGuard;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.event.Event;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultEventService implements EventQueryUseCase, EventAdminUseCase {

    private final EventReaderPort eventReader;
    private final EventStorePort eventStore;
    private final ProductReaderPort productReader;
    private final ImageMediaReferenceGuard imageMediaReferenceGuard;
    private final Clock clock;

    public DefaultEventService(
            EventReaderPort eventReader,
            EventStorePort eventStore,
            ProductReaderPort productReader,
            ImageMediaReferenceGuard imageMediaReferenceGuard,
            Clock clock
    ) {
        this.eventReader = eventReader;
        this.eventStore = eventStore;
        this.productReader = productReader;
        this.imageMediaReferenceGuard = imageMediaReferenceGuard;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> listPublicEvents() {
        return eventReader.findPublicEvents(LocalDateTime.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public Event getPublicEvent(Long id) {
        return eventReader.findPublicById(id, LocalDateTime.now(clock))
                .orElseThrow(NotFoundException.supplier("이벤트"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> listAll() {
        return eventReader.findAllForAdmin();
    }

    @Override
    @Transactional(readOnly = true)
    public Event getForEdit(Long id) {
        return eventReader.findByIdWithRelatedProducts(id)
                .orElseThrow(NotFoundException.supplier("이벤트"));
    }

    @Override
    public Event create(CreateCommand command) {
        Event event = new Event(
                command.title(),
                command.summary(),
                command.content(),
                command.imageUrl(),
                command.startAt(),
                command.endAt(),
                command.published(),
                command.featured(),
                command.relatedProductIds());
        imageMediaReferenceGuard.validateAssignment(event.getImageUrl());
        requireExistingProducts(event.getRelatedProductIds());
        return eventStore.save(event);
    }

    @Override
    public Event update(Long id, UpdateCommand command) {
        Event event = eventReader.findByIdWithRelatedProducts(id)
                .orElseThrow(NotFoundException.supplier("이벤트"));
        requireExpectedVersion(event, command.expectedVersion());
        event.update(
                command.title(),
                command.summary(),
                command.content(),
                command.imageUrl(),
                command.startAt(),
                command.endAt(),
                command.published(),
                command.featured(),
                command.relatedProductIds());
        imageMediaReferenceGuard.validateAssignment(event.getImageUrl());
        requireExistingProducts(event.getRelatedProductIds());
        return eventStore.save(event);
    }

    @Override
    public void delete(Long id, long expectedVersion) {
        Event event = eventReader.findByIdWithRelatedProducts(id)
                .orElseThrow(NotFoundException.supplier("이벤트"));
        requireExpectedVersion(event, expectedVersion);
        eventStore.deleteById(id);
    }

    private void requireExistingProducts(Set<Long> relatedProductIds) {
        if (relatedProductIds.isEmpty()) {
            return;
        }
        Set<Long> foundIds = productReader.findAllById(relatedProductIds).stream()
                .map(product -> product.getId())
                .collect(Collectors.toSet());
        if (!foundIds.equals(relatedProductIds)) {
            throw new NotFoundException("연관 상품");
        }
    }

    private void requireExpectedVersion(Event event, long expectedVersion) {
        if (event.getVersion() != expectedVersion) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT,
                    "다른 관리자가 이벤트를 먼저 수정했습니다. 최신 내용을 다시 불러온 뒤 처리해주세요.");
        }
    }
}
