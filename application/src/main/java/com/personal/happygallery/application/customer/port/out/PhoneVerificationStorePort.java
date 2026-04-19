package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.booking.PhoneVerification;

public interface PhoneVerificationStorePort {
    PhoneVerification save(PhoneVerification phoneVerification);
}
