package com.personal.happygallery.application.policy.port.out;

import com.personal.happygallery.domain.policy.PolicyConsent;

public interface PolicyConsentStorePort {

    PolicyConsent save(PolicyConsent consent);
}
