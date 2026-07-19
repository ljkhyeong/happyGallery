package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationStorePort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PhoneVerificationRetentionService {

    private final PhoneVerificationStorePort verificationStore;

    public PhoneVerificationRetentionService(PhoneVerificationStorePort verificationStore) {
        this.verificationStore = verificationStore;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteExpiredBefore(LocalDateTime cutoff) {
        return verificationStore.deleteExpiredBefore(cutoff);
    }
}
