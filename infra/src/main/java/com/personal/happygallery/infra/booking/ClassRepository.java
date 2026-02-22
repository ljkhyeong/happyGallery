package com.personal.happygallery.infra.booking;

import com.personal.happygallery.domain.booking.BookingClass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassRepository extends JpaRepository<BookingClass, Long> {
}
