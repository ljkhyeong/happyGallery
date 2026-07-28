package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PhoneVerificationStorePort {
    PhoneVerification save(PhoneVerification phoneVerification);

    Optional<PhoneVerification> findByIdForUpdate(
            Long verificationId,
            String phone,
            PhoneVerificationPurpose purpose);

    void invalidateEarlierUnconsumedForPhone(
            String phone,
            PhoneVerificationPurpose purpose,
            Long verificationId);

    int deleteExpiredBefore(LocalDateTime cutoff, int limit);
}
