package com.personal.happygallery.adapter.out.external.customer;

import com.personal.happygallery.application.customer.port.out.CustomerSessionRevocationPort;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

@Component
public class SpringCustomerSessionRevocationAdapter implements CustomerSessionRevocationPort {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public SpringCustomerSessionRevocationAdapter(
            FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public void revokeCredentialVersion(Long userId, long credentialVersion) {
        sessionRepository.findByPrincipalName(userId + ":" + credentialVersion)
                .keySet()
                .forEach(sessionRepository::deleteById);
    }
}
