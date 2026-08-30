package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.domain.booking.Guest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaGuestStoreAdapter implements GuestStorePort {

    private final GuestRepository guestRepository;

    JpaGuestStoreAdapter(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    @Override
    public Guest save(Guest guest) {
        return guestRepository.save(guest);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Guest getOrCreateByPhoneHmac(Guest candidate) {
        // 중복이면 기존 신원 정보를 유지하고 UNIQUE 제약의 직렬화만 이용한다.
        guestRepository.insertIfAbsent(
                candidate.getNameEnc(),
                candidate.getNameHmac(),
                candidate.getPhoneEnc(),
                candidate.getPhoneHmac(),
                candidate.isPhoneVerified());

        return guestRepository.findByPhoneHmac(candidate.getPhoneHmac()).orElseThrow();
    }
}
