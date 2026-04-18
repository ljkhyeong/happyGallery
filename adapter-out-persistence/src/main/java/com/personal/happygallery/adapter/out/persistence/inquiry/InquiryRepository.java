package com.personal.happygallery.adapter.out.persistence.inquiry;

import com.personal.happygallery.application.inquiry.port.out.InquiryReaderPort;
import com.personal.happygallery.application.inquiry.port.out.InquiryStorePort;
import com.personal.happygallery.domain.inquiry.Inquiry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long>, InquiryReaderPort, InquiryStorePort {

    @Override Optional<Inquiry> findById(Long id);
    @Override Inquiry save(Inquiry inquiry);

    List<Inquiry> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Inquiry> findAllByOrderByCreatedAtDesc();

    @Override
    default List<Inquiry> findByUserId(Long userId) {
        return findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    default List<Inquiry> findAll() {
        return findAllByOrderByCreatedAtDesc();
    }
}
