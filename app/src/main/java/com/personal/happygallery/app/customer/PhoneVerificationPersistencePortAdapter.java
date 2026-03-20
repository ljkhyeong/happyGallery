package com.personal.happygallery.app.customer;

import com.personal.happygallery.app.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.app.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.infra.booking.PhoneVerificationRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class PhoneVerificationPersistencePortAdapter implements PhoneVerificationReaderPort, PhoneVerificationStorePort {

    private final PhoneVerificationRepository repository;

    PhoneVerificationPersistencePortAdapter(PhoneVerificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PhoneVerification> findValidVerification(String phone, String code, LocalDateTime now) {
        return repository.findByPhoneAndCodeAndVerifiedFalseAndExpiresAtAfter(phone, code, now);
    }

    @Override
    public PhoneVerification save(PhoneVerification phoneVerification) {
        return repository.save(phoneVerification);
    }
}
