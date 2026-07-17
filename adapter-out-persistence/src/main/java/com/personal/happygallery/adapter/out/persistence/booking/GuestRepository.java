package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.customer.port.out.GuestReaderPort;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.domain.booking.Guest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long>, GuestReaderPort, GuestStorePort {

    @Override Optional<Guest> findById(Long id);
    @Override Guest save(Guest guest);

    /** 블라인드 인덱스로 게스트 조회 */
    @Override Optional<Guest> findByPhoneHmac(String phoneHmac);
}
