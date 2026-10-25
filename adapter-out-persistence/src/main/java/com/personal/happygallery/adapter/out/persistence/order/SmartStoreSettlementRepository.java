package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.SmartStoreSettlementPort;
import com.personal.happygallery.domain.order.SmartStoreSettlementEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SmartStoreSettlementRepository
        extends JpaRepository<SmartStoreSettlementEntry, String>, SmartStoreSettlementPort {

    @Override
    default Optional<SmartStoreSettlementEntry> findByEntryKey(String entryKey) {
        return findById(entryKey);
    }

    @Query("""
            select entry
              from SmartStoreSettlementEntry entry
             where entry.reconciliationStatus not in (
                       com.personal.happygallery.domain.order.SmartStoreSettlementStatus.MATCHED,
                       com.personal.happygallery.domain.order.SmartStoreSettlementStatus.NOT_APPLICABLE)
             order by entry.fetchedAt desc, entry.entryKey
            """)
    List<SmartStoreSettlementEntry> findIssuesPage(Pageable pageable);

    @Override
    default List<SmartStoreSettlementEntry> findIssues(int limit) {
        return findIssuesPage(PageRequest.ofSize(limit));
    }

    @Override
    <S extends SmartStoreSettlementEntry> S save(S entry);
}
