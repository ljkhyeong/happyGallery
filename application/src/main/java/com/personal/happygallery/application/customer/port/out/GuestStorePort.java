package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.booking.Guest;

public interface GuestStorePort {

    Guest save(Guest guest);

    /**
     * 전화번호 HMAC이 같은 Guest가 있으면 재사용하고, 없으면 후보 Guest를 저장한다.
     */
    Guest getOrCreateByPhoneHmac(Guest candidate);
}
