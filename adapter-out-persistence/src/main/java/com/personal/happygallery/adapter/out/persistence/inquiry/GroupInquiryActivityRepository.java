package com.personal.happygallery.adapter.out.persistence.inquiry;

import com.personal.happygallery.application.inquiry.port.out.GroupInquiryActivityPort;
import com.personal.happygallery.domain.inquiry.GroupInquiryActivity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInquiryActivityRepository extends JpaRepository<GroupInquiryActivity, Long>, GroupInquiryActivityPort {
    @Override
    List<GroupInquiryActivity> findByInquiryIdOrderByIdDesc(Long inquiryId);
}
