package com.personal.happygallery.application.inquiry.port.out;

import com.personal.happygallery.domain.inquiry.GroupInquiry;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GroupInquiryPort {
    GroupInquiry saveAndFlush(GroupInquiry inquiry);
    Optional<GroupInquiry> findById(Long id);
    Optional<GroupInquiry> findByIdForUpdate(Long id);
    List<GroupInquiry> search(Long userId, GroupInquiryStatus status, LocalDateTime before, Long beforeId, int limit);
    List<GroupInquiry> findFollowUps(LocalDate today, LocalDate after, Long afterId, int limit);
    void deleteByUserId(Long userId);
}
