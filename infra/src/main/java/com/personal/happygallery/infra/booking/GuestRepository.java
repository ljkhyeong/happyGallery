package com.personal.happygallery.infra.booking;

import com.personal.happygallery.domain.booking.Guest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    /** 전화번호로 게스트 조회 — upsert 패턴용 */
    Optional<Guest> findByPhone(String phone);
}
