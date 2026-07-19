package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.booking.PhoneVerification;
import java.util.Optional;

public interface PhoneVerificationStorePort {
    PhoneVerification save(PhoneVerification phoneVerification);

    Optional<PhoneVerification> findByIdForUpdate(Long verificationId, String phone);

    void invalidateEarlierUnconsumedForPhone(String phone, Long verificationId);
}
