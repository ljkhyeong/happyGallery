package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.out.EmailVerificationStorePort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationRetentionService {

    private final EmailVerificationStorePort verificationStore;

    public EmailVerificationRetentionService(EmailVerificationStorePort verificationStore) {
        this.verificationStore = verificationStore;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteBatchBefore(LocalDateTime cutoff, int limit) {
        return verificationStore.deleteExpiredBefore(cutoff, limit);
    }
}
