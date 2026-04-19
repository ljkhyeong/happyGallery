package com.personal.happygallery.application.inquiry.port.out;

import com.personal.happygallery.domain.inquiry.Inquiry;
import java.util.List;
import java.util.Optional;

public interface InquiryReaderPort {

    Optional<Inquiry> findById(Long id);

    List<Inquiry> findByUserId(Long userId);

    List<Inquiry> findAll();
}
