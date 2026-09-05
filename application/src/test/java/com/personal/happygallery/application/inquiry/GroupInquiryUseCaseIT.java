package com.personal.happygallery.application.inquiry;

import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.inquiry.GroupInquiry;
import com.personal.happygallery.domain.inquiry.GroupInquiryDetails;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UseCaseIT
class GroupInquiryUseCaseIT {
    @TestBean(name = "fixedClock", methodName = "inquiryClock") Clock clock;
    static Clock inquiryClock() {
        return Clock.fixed(Instant.parse("2026-09-05T15:00:00Z"), ZoneOffset.UTC);
    }
    @Autowired GroupInquiryUseCase inquiries;
    @Autowired UserStorePort users;
    @Autowired CustomerAccountLifecycleUseCase lifecycle;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestCleanupSupport cleanup;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM group_inquiries");
        cleanup.clearUsers();
    }

    @Test
    @DisplayName("단체 문의는 연락처를 암호화하고 회원 본인의 이력만 페이지로 조회한다")
    void create_encryptsAndScopesMemberHistory() {
        var owner = users.save(new User("group@example.com", "hash", "회원", "01012345678"));
        var other = users.save(new User("group2@example.com", "hash", "다른 회원", "01099998888"));
        inquiries.create(null, details("비회원 기관"));
        inquiries.create(other.getId(), details("다른 기관"));
        var first = inquiries.create(owner.getId(), details("첫 기관"));
        var second = inquiries.create(owner.getId(), details("둘째 기관"));
        var page = inquiries.listForMember(owner.getId(), null, 1);
        assertThat(page.content()).singleElement().satisfies(view -> assertThat(view.inquiry().getId()).isEqualTo(second.inquiry().getId()));
        assertThat(page.hasMore()).isTrue();
        assertThat(inquiries.listForMember(owner.getId(), page.nextCursor(), 1).content())
                .singleElement().satisfies(view -> assertThat(view.inquiry().getId()).isEqualTo(first.inquiry().getId()));
        String stored = jdbc.queryForObject("SELECT details_enc FROM group_inquiries WHERE id = ?", String.class, first.inquiry().getId());
        assertThat(stored).startsWith("hg:").doesNotContain("01012345678", "담당자", "첫 기관");
        assertThat(inquiries.detailForAdmin(first.inquiry().getId()).view().details().phone()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("상담은 접수에서 상담 중을 거쳐 확정하고 이전 화면의 변경은 거절한다")
    void consultation_checksStatusAndVersionAndRetainsActivities() {
        var inquiry = inquiries.createExternal(99L, details("외부 기관"));
        long id = inquiry.view().inquiry().getId();
        assertThat(inquiry.view().inquiry().getSource()).isEqualTo(GroupInquiry.Source.EXTERNAL);
        assertThatThrownBy(() -> inquiries.update(id, 0, GroupInquiryStatus.CONFIRMED, "바로 확정", 99L))
                .isInstanceOf(HappyGalleryException.class);
        var consulting = inquiries.update(id, 0, GroupInquiryStatus.CONSULTING, "일정과 재료 상담", 99L);
        assertThat(consulting.view().inquiry().getVersion()).isEqualTo(1);
        assertThatThrownBy(() -> inquiries.update(id, 0, GroupInquiryStatus.CLOSED, "오래된 화면", 98L))
                .isInstanceOf(HappyGalleryException.class).hasMessageContaining("다른 관리자");
        var confirmed = inquiries.update(id, 1, GroupInquiryStatus.CONFIRMED, "20명 레진 수업 확정", 98L);
        assertThat(confirmed.activities()).hasSize(3);
        assertThat(confirmed.activities().getFirst().note()).isEqualTo("20명 레진 수업 확정");
        assertThat(confirmed.activities().getFirst().activity().getAdminId()).isEqualTo(98);
        assertThat(inquiries.listForAdmin(GroupInquiryStatus.RECEIVED, null, 20).content()).isEmpty();
        assertThat(inquiries.listForAdmin(GroupInquiryStatus.CONFIRMED, null, 20).content()).hasSize(1);
        assertThat(jdbc.queryForList("SELECT note_enc FROM group_inquiry_activities", String.class))
                .allSatisfy(value -> assertThat(value).startsWith("hg:").doesNotContain("상담", "확정"));
        var reopened = inquiries.update(id, 2, GroupInquiryStatus.CONSULTING, "일정 재협의", 99L);
        assertThat(reopened.view().inquiry().getStatus()).isEqualTo(GroupInquiryStatus.CONSULTING);
    }

    @Test
    @DisplayName("회원 탈퇴 시 단체 문의와 상담 메모를 함께 삭제한다")
    void withdrawal_erasesPrivateInquiryAndActivities() {
        var owner = users.save(new User("withdraw-group@example.com", "hash", "회원", "01012345678"));
        var inquiry = inquiries.create(owner.getId(), details("탈퇴 기관"));
        inquiries.update(inquiry.inquiry().getId(), 0, GroupInquiryStatus.CONSULTING, "연락처 상담", 99L);
        lifecycle.withdraw(new CustomerAccountLifecycleUseCase.WithdrawCommand(owner.getId(), owner.getCredentialVersion(), true));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_inquiries", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_inquiry_activities", Integer.class)).isZero();
    }

    @Test
    @DisplayName("서울 날짜로 오늘과 지난 연락을 순서대로 조회하고 종료·해제된 상담은 제외한다")
    void followUpsRespectSeoulDateAndClosure() {
        var overdue = inquiries.create(null, details("지난 연락"));
        var today = inquiries.create(null, details("오늘 연락"));
        var future = inquiries.create(null, details("내일 연락"));
        var closed = inquiries.create(null, details("종료 문의"));
        var todayDate = LocalDate.of(2026, 9, 6);
        inquiries.scheduleContact(overdue.inquiry().getId(), 0, todayDate.minusDays(1), 99L);
        var scheduled = inquiries.scheduleContact(today.inquiry().getId(), 0, todayDate, 99L);
        inquiries.scheduleContact(future.inquiry().getId(), 0, todayDate.plusDays(1), 99L);
        inquiries.scheduleContact(closed.inquiry().getId(), 0, todayDate, 99L);
        inquiries.update(closed.inquiry().getId(), 1, GroupInquiryStatus.CLOSED, "상담 종료", 99L);
        var first = inquiries.followUps(null, 1);
        assertThat(first.content()).singleElement().satisfies(row -> assertThat(row.inquiry().getId()).isEqualTo(overdue.inquiry().getId()));
        assertThat(first.hasMore()).isTrue();
        var second = inquiries.followUps(first.nextCursor(), 1);
        assertThat(second.content()).singleElement().satisfies(row -> assertThat(row.inquiry().getId()).isEqualTo(today.inquiry().getId()));
        assertThat(second.hasMore()).isFalse();
        assertThatThrownBy(() -> inquiries.scheduleContact(today.inquiry().getId(), 0, null, 99L)).isInstanceOf(HappyGalleryException.class);
        assertThat(scheduled.activities().getFirst().note()).contains("2026-09-06");
        inquiries.scheduleContact(today.inquiry().getId(), 1, null, 99L);
        assertThat(inquiries.followUps(null, 20).content()).hasSize(1);
        assertThat(inquiries.detailForAdmin(closed.inquiry().getId()).view().inquiry().getNextContactOn()).isNull();
        assertThatThrownBy(() -> inquiries.scheduleContact(closed.inquiry().getId(), 2, todayDate, 99L))
                .isInstanceOf(HappyGalleryException.class);
    }

    private GroupInquiryDetails details(String organization) {
        return new GroupInquiryDetails(organization, "담당자", "010-1234-5678", null,
                20, "9월 평일 오전", "기관 강당", "레진아트", "초등학생 대상 수업");
    }
}
