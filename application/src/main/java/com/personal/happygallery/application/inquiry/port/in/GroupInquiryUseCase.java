package com.personal.happygallery.application.inquiry.port.in;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.inquiry.GroupInquiry;
import com.personal.happygallery.domain.inquiry.GroupInquiryActivity;
import com.personal.happygallery.domain.inquiry.GroupInquiryDetails;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import java.util.List;
import java.time.LocalDate;

public interface GroupInquiryUseCase {
    record View(GroupInquiry inquiry, GroupInquiryDetails details) {}
    record ActivityView(GroupInquiryActivity activity, String note) {}
    record Detail(View view, List<ActivityView> activities) {}

    View create(Long userId, GroupInquiryDetails details);
    Detail createExternal(Long adminId, GroupInquiryDetails details);
    CursorPage<View> listForAdmin(GroupInquiryStatus status, String cursor, int size);
    CursorPage<View> listForMember(Long userId, String cursor, int size);
    Detail detailForAdmin(Long id);
    Detail scheduleContact(Long id, long version, LocalDate nextContactOn, Long adminId);
    CursorPage<View> followUps(String cursor, int size);
    Detail update(Long id, long version, GroupInquiryStatus status, String note, Long adminId);
}
