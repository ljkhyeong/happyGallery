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
    record MemberDetail(View view, List<ActivityView> changes) {}
    record AdminFilter(GroupInquiryStatus status, GroupInquiry.Source source, Long inquiryId,
                       LocalDate from, LocalDate to) {}
    record Detail(View view, List<ActivityView> activities) {}

    View create(Long userId, GroupInquiryDetails details);
    Detail createExternal(Long adminId, GroupInquiryDetails details);
    CursorPage<View> listForAdmin(AdminFilter filter, String cursor, int size);
    CursorPage<View> listForMember(Long userId, String cursor, int size);
    MemberDetail detailForMember(Long userId, Long id);
    MemberDetail reviseByMember(Long userId, Long id, long version, int headcount, String preferredSchedule);
    MemberDetail cancelByMember(Long userId, Long id, long version);
    Detail detailForAdmin(Long id);
    Detail scheduleContact(Long id, long version, LocalDate nextContactOn, Long adminId);
    CursorPage<View> followUps(String cursor, int size);
    Detail update(Long id, long version, GroupInquiryStatus status, String note, Long adminId);
}
