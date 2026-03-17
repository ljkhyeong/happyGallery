package com.personal.happygallery.infra.inquiry;

import com.personal.happygallery.domain.inquiry.Inquiry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Inquiry> findAllByOrderByCreatedAtDesc();
}
