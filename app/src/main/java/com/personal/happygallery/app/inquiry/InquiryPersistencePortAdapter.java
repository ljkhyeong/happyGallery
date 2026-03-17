package com.personal.happygallery.app.inquiry;

import com.personal.happygallery.app.inquiry.port.out.InquiryReaderPort;
import com.personal.happygallery.app.inquiry.port.out.InquiryStorePort;
import com.personal.happygallery.domain.inquiry.Inquiry;
import com.personal.happygallery.infra.inquiry.InquiryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class InquiryPersistencePortAdapter implements InquiryReaderPort, InquiryStorePort {

    private final InquiryRepository repository;

    InquiryPersistencePortAdapter(InquiryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Inquiry> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Inquiry> findByUserId(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Inquiry> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Inquiry save(Inquiry inquiry) {
        return repository.save(inquiry);
    }
}
