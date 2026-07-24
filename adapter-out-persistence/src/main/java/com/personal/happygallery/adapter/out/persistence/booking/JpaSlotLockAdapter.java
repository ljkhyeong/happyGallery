package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.SlotLockPort;
import com.personal.happygallery.domain.booking.Slot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class JpaSlotLockAdapter implements SlotLockPort {

    private final EntityManager entityManager;

    JpaSlotLockAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Slot> lockAllById(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        entityManager.flush();
        List<Long> lockedIds = lockIds(ids);
        detachReferences(lockedIds);
        return loadByIds(lockedIds);
    }

    @Override
    public List<Slot> lockScope(Long classId,
                               Long sourceSlotId,
                               LocalDateTime windowStart,
                               LocalDateTime windowEnd) {
        entityManager.flush();
        @SuppressWarnings("unchecked")
        List<Number> lockedRows = entityManager.createNativeQuery(
                        """
                        SELECT id
                        FROM slots
                        WHERE class_id = :classId
                          AND (id = :sourceSlotId
                               OR (start_at >= :windowStart AND start_at < :windowEnd))
                        ORDER BY id
                        FOR UPDATE
                        """)
                .setParameter("classId", classId)
                .setParameter("sourceSlotId", sourceSlotId)
                .setParameter("windowStart", windowStart)
                .setParameter("windowEnd", windowEnd)
                .getResultList();
        List<Long> ids = lockedRows.stream().map(Number::longValue).toList();
        detachReferences(ids);
        return loadByIds(ids);
    }

    @SuppressWarnings("unchecked")
    private List<Long> lockIds(List<Long> ids) {
        List<Number> lockedRows = entityManager.createNativeQuery(
                        "SELECT id FROM slots WHERE id IN (:ids) ORDER BY id FOR UPDATE")
                .setParameter("ids", ids)
                .getResultList();
        return lockedRows.stream().map(Number::longValue).toList();
    }

    private void detachReferences(List<Long> ids) {
        ids.forEach(id -> entityManager.detach(entityManager.getReference(Slot.class, id)));
    }

    private List<Slot> loadByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery(
                        "SELECT s FROM Slot s WHERE s.id IN :ids ORDER BY s.id", Slot.class)
                .setParameter("ids", ids)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }
}
