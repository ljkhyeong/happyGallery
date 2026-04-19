package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.DevPhoneVerificationQueryUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.domain.booking.PhoneVerification;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("local")
@Service
public class DefaultDevPhoneVerificationQueryService implements DevPhoneVerificationQueryUseCase {

    private final PhoneVerificationReaderPort phoneVerificationReader;

    public DefaultDevPhoneVerificationQueryService(PhoneVerificationReaderPort phoneVerificationReader) {
        this.phoneVerificationReader = phoneVerificationReader;
    }

    @Override
    public Optional<String> findLatestUnverifiedCode(String phone) {
        return phoneVerificationReader.findLatestUnverifiedCode(phone)
                .map(PhoneVerification::getCode);
    }
}
