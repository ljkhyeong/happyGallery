package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.customer.port.out.GuestReaderPort;
import com.personal.happygallery.domain.booking.Guest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestRepository extends JpaRepository<Guest, Long>, GuestReaderPort {

    @Override Optional<Guest> findById(Long id);

    /** 블라인드 인덱스로 게스트 조회 */
    @Override Optional<Guest> findByPhoneHmac(String phoneHmac);

    /** UNIQUE phone_hmac 제약을 이용해 최초 게스트만 원자적으로 생성한다. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO guests (name_enc, name_hmac, phone_enc, phone_hmac, phone_verified)
            VALUES (:nameEnc, :nameHmac, :phoneEnc, :phoneHmac, :phoneVerified)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int insertIfAbsent(@Param("nameEnc") String nameEnc,
                       @Param("nameHmac") String nameHmac,
                       @Param("phoneEnc") String phoneEnc,
                       @Param("phoneHmac") String phoneHmac,
                       @Param("phoneVerified") boolean phoneVerified);
}
