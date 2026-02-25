package com.personal.happygallery.infra.booking;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.RefundStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByStatus(RefundStatus status);
}
