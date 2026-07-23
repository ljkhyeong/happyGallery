package com.personal.happygallery.application.search;

import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.passPurchase;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.search.dto.AdminPassStatus;
import com.personal.happygallery.application.search.dto.AdminPassView;
import com.personal.happygallery.application.search.port.in.AdminPassQueryUseCase;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@UseCaseIT
class AdminPassQueryUseCaseIT {

    @Autowired AdminPassQueryUseCase queryUseCase;
    @Autowired UserStorePort userStorePort;
    @Autowired PassPurchaseStorePort passPurchaseStorePort;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired BookingStorePort bookingStorePort;
    @Autowired RefundPort refundPort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired MockMvc mockMvc;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @DisplayName("관리자는 회원명, 정규화 전화번호, 표시용 8회권 번호로 같은 8회권을 검색한다")
    @Test
    void searchByCustomerIdentityAndPassNumber() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        User targetUser = saveUser("target-pass@example.com", "검색 회원", "010-1234-5678");
        PassPurchase targetPass = passPurchaseStorePort.save(
                passPurchase(targetUser.getId(), now.plusDays(90), 320_000L));
        saveFuturePassBooking(targetUser.getId(), targetPass, now.plusDays(3));

        User otherUser = saveUser("other-pass@example.com", "다른 회원", "01099998888");
        passPurchaseStorePort.save(passPurchase(otherUser.getId(), now.plusDays(60), 240_000L));

        OffsetPage<AdminPassView> byName = queryUseCase.search("검색 회원", 0, 20);
        OffsetPage<AdminPassView> byPhone = queryUseCase.search("010 1234 5678", 0, 20);
        OffsetPage<AdminPassView> byNumber = queryUseCase.search(
                "PASS-%08d".formatted(targetPass.getId()), 0, 20);
        AdminPassView pass = byPhone.content().getFirst();

        assertSoftly(softly -> {
            softly.assertThat(byName.content()).extracting(AdminPassView::passId)
                    .containsExactly(targetPass.getId());
            softly.assertThat(byPhone.content()).extracting(AdminPassView::passId)
                    .containsExactly(targetPass.getId());
            softly.assertThat(byNumber.content()).extracting(AdminPassView::passId)
                    .containsExactly(targetPass.getId());
            softly.assertThat(pass.passNumber()).isEqualTo("PASS-%08d".formatted(targetPass.getId()));
            softly.assertThat(pass.customerName()).isEqualTo("검색 회원");
            softly.assertThat(pass.customerPhone()).isEqualTo("01012345678");
            softly.assertThat(pass.status()).isEqualTo(AdminPassStatus.ACTIVE);
            softly.assertThat(pass.remainingCredits()).isEqualTo(7);
            softly.assertThat(pass.futureBookingCount()).isEqualTo(1);
            softly.assertThat(pass.expectedRefundAmount()).isEqualTo(320_000L);
            softly.assertThat(pass.refundStatus()).isNull();
        });

        mockMvc.perform(get("/api/v1/admin/passes/search")
                        .param("keyword", "010-1234-5678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passId").value(targetPass.getId()))
                .andExpect(jsonPath("$.content[0].customerName").value("검색 회원"))
                .andExpect(jsonPath("$.content[0].customerPhone").value("010****5678"))
                .andExpect(jsonPath("$.content[0].futureBookingCount").value(1))
                .andExpect(jsonPath("$.content[0].expectedRefundAmount").value(320_000));
    }

    @DisplayName("환불이 접수된 8회권 상세는 만료나 소진보다 환불 진행 상태를 우선한다")
    @Test
    void getPass_prioritizesRefundStatus() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = saveUser("refund-pass@example.com", "환불 회원", "01022223333");
        PassPurchase pass = passPurchase(user.getId(), now.minusDays(1), 320_000L);
        pass.expire();
        pass = passPurchaseStorePort.save(pass);
        AdminPassView expiredDetail = queryUseCase.get(pass.getId());
        refundPort.save(Refund.forPass(pass.getId(), 120_000L, "payment-key"));

        AdminPassView detail = queryUseCase.get(pass.getId());

        assertSoftly(softly -> {
            softly.assertThat(expiredDetail.status()).isEqualTo(AdminPassStatus.EXPIRED);
            softly.assertThat(expiredDetail.expectedRefundAmount()).isZero();
            softly.assertThat(detail.status()).isEqualTo(AdminPassStatus.REFUND_PENDING);
            softly.assertThat(detail.refundStatus()).isEqualTo(RefundStatus.REQUESTED);
            softly.assertThat(detail.expectedRefundAmount()).isEqualTo(120_000L);
            softly.assertThat(detail.remainingCredits()).isZero();
        });

        mockMvc.perform(get("/api/v1/admin/passes/{passId}", pass.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUND_PENDING"))
                .andExpect(jsonPath("$.refundStatus").value("REQUESTED"))
                .andExpect(jsonPath("$.customerPhone").value("010****3333"));
    }

    private User saveUser(String email, String name, String phone) {
        return userStorePort.save(new User(email, "hashed-password", name, phone));
    }

    private void saveFuturePassBooking(Long userId, PassPurchase pass, LocalDateTime startAt) {
        BookingClass bookingClass = classStorePort.save(
                bookingClass("우드 정규 클래스", "WOOD", 120, 50_000L, 30));
        Slot savedSlot = slotStorePort.save(slot(bookingClass, startAt, startAt.plusHours(2)));
        pass.useCredit(LocalDateTime.now(clock));
        passPurchaseStorePort.save(pass);
        bookingStorePort.save(Booking.forMemberPass(userId, savedSlot, pass));
    }

    private void cleanup() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }
}
