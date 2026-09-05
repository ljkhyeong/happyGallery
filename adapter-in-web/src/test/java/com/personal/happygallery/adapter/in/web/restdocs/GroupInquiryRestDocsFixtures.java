package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.domain.inquiry.GroupInquiry;
import com.personal.happygallery.domain.inquiry.GroupInquiryActivity;
import com.personal.happygallery.domain.inquiry.GroupInquiryDetails;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

final class GroupInquiryRestDocsFixtures {
    static final String REQUEST = """
            {"organization":"충주 기관","contactName":"담당자","phone":"01012345678","email":null,
            "headcount":20,"preferredSchedule":"9월 평일 오전","location":"기관 강당","classInterest":"레진아트","message":"초등학생 대상"}
            """;

    static GroupInquiryUseCase.Detail detail() {
        var now = LocalDateTime.of(2026, 9, 5, 10, 0);
        var inquiry = new GroupInquiry(11L, GroupInquiry.Source.WEBSITE, "encrypted", now);
        ReflectionTestUtils.setField(inquiry, "id", 51L);
        var details = new GroupInquiryDetails("충주 기관", "담당자", "01012345678", null,
                20, "9월 평일 오전", "기관 강당", "레진아트", "초등학생 대상");
        var activity = new GroupInquiryActivity(51L, 99L, GroupInquiryStatus.RECEIVED,
                GroupInquiryStatus.CONSULTING, "encrypted", now);
        ReflectionTestUtils.setField(activity, "id", 61L);
        return new GroupInquiryUseCase.Detail(new GroupInquiryUseCase.View(inquiry, details),
                List.of(new GroupInquiryUseCase.ActivityView(activity, "일정 협의 중")));
    }
}
