package com.personal.happygallery.application.inquiry.port.out;

import com.personal.happygallery.domain.inquiry.Inquiry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InquiryReaderPort {

    Optional<Inquiry> findById(Long id);

    Optional<Inquiry> findByIdForUpdate(Long id);

    List<Inquiry> findByUserId(Long userId, int limit);

    List<Inquiry> findByUserIdAfter(
            Long userId, LocalDateTime createdAt, Long id, int limit);

    List<Inquiry> findRecent(int limit);

    List<Inquiry> findRecentAfter(LocalDateTime createdAt, Long id, int limit);
}
