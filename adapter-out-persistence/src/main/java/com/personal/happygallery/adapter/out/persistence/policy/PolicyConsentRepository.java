package com.personal.happygallery.adapter.out.persistence.policy;

import com.personal.happygallery.application.policy.port.out.PolicyConsentStorePort;
import com.personal.happygallery.domain.policy.PolicyConsent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyConsentRepository
        extends JpaRepository<PolicyConsent, Long>, PolicyConsentStorePort {

    @Override
    PolicyConsent save(PolicyConsent consent);

    List<PolicyConsent> findByUserIdOrderById(Long userId);

    List<PolicyConsent> findByPaymentAttemptIdOrderById(Long paymentAttemptId);
}
