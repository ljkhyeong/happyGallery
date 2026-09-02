package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.SmartStoreOrderMappingHistoryPort;
import com.personal.happygallery.domain.product.SmartStoreOrderMappingHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmartStoreOrderMappingHistoryRepository
        extends JpaRepository<SmartStoreOrderMappingHistory, Long>, SmartStoreOrderMappingHistoryPort {

    @Override
    @Query(value = """
            SELECT *
              FROM smartstore_order_mapping_history
             WHERE origin_product_no = :originProductNo
               AND ((:optionId IS NULL AND option_id IS NULL) OR option_id = :optionId)
               AND enabled = TRUE
               AND closed_at >= :orderedAt
             ORDER BY closed_at ASC, id ASC
             LIMIT 1
            """, nativeQuery = true)
    Optional<SmartStoreOrderMappingHistory> findResolvable(
            @Param("originProductNo") Long originProductNo,
            @Param("optionId") Long optionId,
            @Param("orderedAt") LocalDateTime orderedAt);

    @Override
    <S extends SmartStoreOrderMappingHistory> List<S> saveAll(Iterable<S> mappings);
}
