package com.personal.happygallery.infra.booking;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.RefundStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    @Query("SELECT r FROM Refund r LEFT JOIN FETCH r.booking WHERE r.status = :status")
    List<Refund> findByStatus(@Param("status") RefundStatus status);
}
