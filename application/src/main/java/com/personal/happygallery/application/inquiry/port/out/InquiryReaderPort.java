package com.personal.happygallery.application.inquiry.port.out;

import com.personal.happygallery.domain.inquiry.Inquiry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InquiryReaderPort {

    Optional<Inquiry> findByIdForUpdate(Long id);

    Optional<Inquiry> findByIdAndUserId(Long id, Long userId);

    /** 커서 값이 null이면 첫 페이지를 조회한다. 정렬은 (createdAt DESC, id DESC)다. */
    List<Inquiry> findByUserId(Long userId, LocalDateTime cursorCreatedAt, Long cursorId, int limit);

    /** 커서 값이 null이면 첫 페이지를 조회한다. 정렬은 (createdAt DESC, id DESC)다. */
    List<Inquiry> findRecent(LocalDateTime cursorCreatedAt, Long cursorId, int limit);
}
