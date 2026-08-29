package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.SmartStoreSettlementPort;
import com.personal.happygallery.domain.order.SmartStoreSettlementEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmartStoreSettlementRepository
        extends JpaRepository<SmartStoreSettlementEntry, String>, SmartStoreSettlementPort {

    @Override
    default Optional<SmartStoreSettlementEntry> findByEntryKey(String entryKey) {
        return findById(entryKey);
    }

    @Override
    @Query(value = """
            select *
              from smartstore_settlement_entries
             where reconciliation_status not in ('MATCHED', 'NOT_APPLICABLE')
             order by fetched_at desc, entry_key
             limit :limit
            """, nativeQuery = true)
    List<SmartStoreSettlementEntry> findIssues(@Param("limit") int limit);

    @Override
    <S extends SmartStoreSettlementEntry> S save(S entry);
}
