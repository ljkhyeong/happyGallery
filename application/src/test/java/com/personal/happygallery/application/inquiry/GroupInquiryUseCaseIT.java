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
                .isInstanceOf(HappyGalleryException.class).hasMessageContaining("문의가 변경");
        var confirmed = inquiries.update(id, 1, GroupInquiryStatus.CONFIRMED, "20명 레진 수업 확정", 98L);
        assertThat(confirmed.activities()).hasSize(3);
        assertThat(confirmed.activities().getFirst().note()).isEqualTo("20명 레진 수업 확정");
        assertThat(confirmed.activities().getFirst().activity().getAdminId()).isEqualTo(98);
        assertThat(inquiries.listForAdmin(new GroupInquiryUseCase.AdminFilter(GroupInquiryStatus.RECEIVED, null, null, null, null), null, 20).content()).isEmpty();
        assertThat(inquiries.listForAdmin(new GroupInquiryUseCase.AdminFilter(GroupInquiryStatus.CONFIRMED, null, null, null, null), null, 20).content()).hasSize(1);
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

    @Test
    @DisplayName("회원은 본인 문의의 일정과 인원을 변경하고 취소하며 관리자 메모는 볼 수 없다")
    void memberChangesCheckOwnerVersionAndHistory() {
        var owner = users.save(new User("edit-group@example.com", "hash", "회원", "01012345678"));
        var other = users.save(new User("edit-other@example.com", "hash", "다른 회원", "01099998888"));
        var id = inquiries.create(owner.getId(), details("수정 기관")).inquiry().getId();
        inquiries.update(id, 0, GroupInquiryStatus.CONSULTING, "로컬 관리자 전용 견적 메모", null);
        assertThatThrownBy(() -> inquiries.detailForMember(other.getId(), id)).hasMessageContaining("찾을 수 없");
        assertThatThrownBy(() -> inquiries.reviseByMember(other.getId(), id, 1, 30, "10월 오전")).hasMessageContaining("찾을 수 없");
        assertThatThrownBy(() -> inquiries.cancelByMember(other.getId(), id, 1)).hasMessageContaining("찾을 수 없");
        var revised = inquiries.reviseByMember(owner.getId(), id, 1, 30, "10월 오전");
        assertThat(revised.view().details().headcount()).isEqualTo(30);
        assertThat(revised.changes()).singleElement().satisfies(change -> assertThat(change.note()).contains("20명 → 30명", "9월 평일 오전 → 10월 오전"));
        assertThat(inquiries.detailForAdmin(id).activities()).hasSize(2);
        assertThatThrownBy(() -> inquiries.reviseByMember(owner.getId(), id, 1, 40, "오래된 일정")).hasMessageContaining("문의가 변경");
        assertThat(inquiries.detailForMember(owner.getId(), id).view().details().headcount()).isEqualTo(30);
        inquiries.scheduleContact(id, 2, LocalDate.of(2026, 9, 6), 99L);
        var canceled = inquiries.cancelByMember(owner.getId(), id, 3);
        assertThat(canceled.view().inquiry().getStatus()).isEqualTo(GroupInquiryStatus.CANCELED);
        assertThat(canceled.view().inquiry().getNextContactOn()).isNull();
        assertThat(canceled.changes()).hasSize(2);
        assertThat(inquiries.followUps(null, 20).content()).isEmpty();
        assertThatThrownBy(() -> inquiries.update(id, 4, GroupInquiryStatus.CONSULTING, "취소 문의 재개", 99L)).isInstanceOf(HappyGalleryException.class);
        assertThatThrownBy(() -> inquiries.reviseByMember(owner.getId(), id, 4, 50, "취소 후 수정")).hasMessageContaining("확정 전");
        assertThat(jdbc.queryForList("SELECT note_enc FROM group_inquiry_activities", String.class))
                .allSatisfy(value -> assertThat(value).startsWith("hg:").doesNotContain("관리자", "10월", "회원"));
        var confirmedId = inquiries.create(owner.getId(), details("확정 기관")).inquiry().getId();
        inquiries.update(confirmedId, 0, GroupInquiryStatus.CONSULTING, "상담", 99L);
        inquiries.update(confirmedId, 1, GroupInquiryStatus.CONFIRMED, "확정", 99L);
        assertThatThrownBy(() -> inquiries.cancelByMember(owner.getId(), confirmedId, 2)).hasMessageContaining("확정 전");
        assertThatThrownBy(() -> inquiries.reviseByMember(owner.getId(), confirmedId, 2, 30, "확정 후 수정")).hasMessageContaining("확정 전");
    }

    @Test
    @DisplayName("관리자는 접수 번호와 경로 및 접수일 종료 시각까지 조회하고 같은 조건으로 다음 페이지를 연다")
    void adminSearchScopesBeforePaging() {
        var before = inquiries.createExternal(99L, details("전날 접수")).view().inquiry().getId();
        var first = inquiries.createExternal(99L, details("시작 시각")).view().inquiry().getId();
        var last = inquiries.createExternal(99L, details("마지막 시각")).view().inquiry().getId();
        var after = inquiries.createExternal(99L, details("다음날 접수")).view().inquiry().getId();
        inquiries.create(null, details("웹 접수"));
        jdbc.update("UPDATE group_inquiries SET created_at = '2026-09-05 23:59:59' WHERE id = ?", before);
        jdbc.update("UPDATE group_inquiries SET created_at = '2026-09-06 00:00:00' WHERE id = ?", first);
        jdbc.update("UPDATE group_inquiries SET created_at = '2026-09-06 23:59:59.999999' WHERE id = ?", last);
        jdbc.update("UPDATE group_inquiries SET created_at = '2026-09-07 00:00:00' WHERE id = ?", after);
        var date = LocalDate.of(2026, 9, 6);
        var filter = new GroupInquiryUseCase.AdminFilter(GroupInquiryStatus.RECEIVED, GroupInquiry.Source.EXTERNAL, null, date, date);
        var page = inquiries.listForAdmin(filter, null, 1);
        assertThat(page.content()).singleElement().satisfies(row -> assertThat(row.inquiry().getId()).isEqualTo(last));
        assertThat(page.hasMore()).isTrue();
        var next = inquiries.listForAdmin(filter, page.nextCursor(), 1);
        assertThat(next.content()).singleElement().satisfies(row -> assertThat(row.inquiry().getId()).isEqualTo(first));
        assertThat(next.hasMore()).isFalse();
        assertThat(inquiries.listForAdmin(new GroupInquiryUseCase.AdminFilter(null, null, first, null, null), null, 20).content()).hasSize(1);
        assertThat(inquiries.listForAdmin(new GroupInquiryUseCase.AdminFilter(null, GroupInquiry.Source.WEBSITE, first, null, null), null, 20).content()).isEmpty();
        assertThatThrownBy(() -> inquiries.listForAdmin(new GroupInquiryUseCase.AdminFilter(null, null, null, date.plusDays(1), date), null, 20))
                .hasMessageContaining("조회 시작일");
    }

    private GroupInquiryDetails details(String organization) {
        return new GroupInquiryDetails(organization, "담당자", "010-1234-5678", null,
                20, "9월 평일 오전", "기관 강당", "레진아트", "초등학생 대상 수업");
    }
}
