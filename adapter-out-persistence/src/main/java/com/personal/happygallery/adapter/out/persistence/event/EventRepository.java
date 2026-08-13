package com.personal.happygallery.adapter.out.persistence.event;

import com.personal.happygallery.application.event.port.out.EventReaderPort;
import com.personal.happygallery.application.event.port.out.EventStorePort;
import com.personal.happygallery.domain.event.Event;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long>, EventReaderPort, EventStorePort {

    @Override
    <S extends Event> S save(S event);

    @Override
    void deleteById(Long id);

    @Override
    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.relatedProductIds
            WHERE e.id = :id
            """)
    Optional<Event> findByIdWithRelatedProducts(@Param("id") Long id);

    @Override
    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.relatedProductIds
            WHERE e.id = :id
              AND e.published = true
              AND e.endAt > :now
            """)
    Optional<Event> findPublicById(
            @Param("id") Long id,
            @Param("now") LocalDateTime now);

    @Override
    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.relatedProductIds
            WHERE e.published = true
              AND e.endAt > :now
            ORDER BY
              CASE WHEN e.startAt <= :now THEN 0 ELSE 1 END,
              e.startAt ASC,
              e.id ASC
            """)
    List<Event> findPublicEvents(@Param("now") LocalDateTime now);

    @Override
    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.relatedProductIds
            ORDER BY e.startAt DESC, e.id DESC
            """)
    List<Event> findAllForAdmin();
}
