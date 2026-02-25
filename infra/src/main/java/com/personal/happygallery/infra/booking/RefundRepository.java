package com.personal.happygallery.infra.booking;

import com.personal.happygallery.domain.booking.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}
