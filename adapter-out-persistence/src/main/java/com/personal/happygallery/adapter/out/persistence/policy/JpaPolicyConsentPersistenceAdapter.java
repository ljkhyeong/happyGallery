package com.personal.happygallery.adapter.out.persistence.policy;

import com.personal.happygallery.application.policy.port.out.PolicyConsentStorePort;
import com.personal.happygallery.domain.policy.PolicyConsent;
import org.springframework.stereotype.Repository;

@Repository
class JpaPolicyConsentPersistenceAdapter implements PolicyConsentStorePort {

    private final PolicyConsentRepository repository;

    JpaPolicyConsentPersistenceAdapter(PolicyConsentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PolicyConsent save(PolicyConsent consent) {
        return repository.save(consent);
    }
}
