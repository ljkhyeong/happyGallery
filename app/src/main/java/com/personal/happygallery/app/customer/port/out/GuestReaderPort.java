package com.personal.happygallery.app.customer.port.out;

import com.personal.happygallery.domain.booking.Guest;
import java.util.Optional;

/**
 * 게스트 조회 포트.
 */
public interface GuestReaderPort {

    Optional<Guest> findById(Long id);

    Optional<Guest> findByPhone(String phone);
}
