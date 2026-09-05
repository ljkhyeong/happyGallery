package com.personal.happygallery.application.inquiry.port.out;

import com.personal.happygallery.domain.inquiry.GroupInquiryActivity;
import java.util.List;

public interface GroupInquiryActivityPort {
    GroupInquiryActivity save(GroupInquiryActivity activity);
    List<GroupInquiryActivity> findByInquiryIdOrderByIdDesc(Long inquiryId);
}
