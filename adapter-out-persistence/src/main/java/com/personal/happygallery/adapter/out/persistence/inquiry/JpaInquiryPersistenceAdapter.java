package com.personal.happygallery.adapter.out.persistence.inquiry;

import com.personal.happygallery.application.inquiry.port.out.InquiryStorePort;
import com.personal.happygallery.domain.inquiry.Inquiry;
import org.springframework.stereotype.Repository;

@Repository
class JpaInquiryPersistenceAdapter implements InquiryStorePort {

    private final InquiryRepository repository;

    JpaInquiryPersistenceAdapter(InquiryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Inquiry save(Inquiry inquiry) {
        return repository.save(inquiry);
    }
}
