package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.booking.port.out.ClassStorePort;
import com.personal.happygallery.app.booking.port.out.SlotStorePort;
import com.personal.happygallery.app.customer.port.out.UserReaderPort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class PassCreditUsageWebUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired CustomerAuthFilter customerAuthFilter;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired UserReaderPort userReaderPort;
    @Autowired PassPurchaseStorePort passPurchaseStorePort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    private PassCreditUsageFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new PassCreditUsageFixture(
                context,
                customerAuthFilter,
                springSessionRepositoryFilter,
                classStorePort,
                slotStorePort,
                userReaderPort,
                passPurchaseStorePort,
                cleanupSupport,
                clock);
        fixture.setUp();
    }

    @DisplayName("8회권으로 예약하면 BOOKED 응답을 반환한다")
    @Test
    void bookWithPass_returnsBookedResponse() throws Exception {
        var slot = fixture.createFutureSlot();

        fixture.mockMvc().perform(post("/api/v1/me/bookings")
                        .cookie(fixture.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slotId": %d,
                                  "passId": %d
                                }
                                """.formatted(slot.getId(), fixture.pass().getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @DisplayName("8회권 예약을 기한 내 취소하면 refundable=true를 반환한다")
    @Test
    void cancelPassBookingTimely_returnsRefundableTrue() throws Exception {
        Long bookingId = fixture.createPassBooking(fixture.createFutureSlot().getId());

        fixture.mockMvc().perform(delete("/api/v1/me/bookings/{id}", bookingId)
                        .cookie(fixture.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true));
    }

    @DisplayName("8회권 예약을 늦게 취소하면 refundable=false를 반환한다")
    @Test
    void cancelPassBookingLate_returnsRefundableFalse() throws Exception {
        LocalDateTime today14 = LocalDateTime.now(fixture.clock()).toLocalDate().atTime(14, 0);
        Long bookingId = fixture.createPassBooking(fixture.createFutureSlot(today14).getId());

        fixture.mockMvc().perform(delete("/api/v1/me/bookings/{id}", bookingId)
                        .cookie(fixture.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(false));
    }

    @DisplayName("노쇼 처리 요청은 NO_SHOW 응답을 반환한다")
    @Test
    void markNoShow_returnsNoShowResponse() throws Exception {
        Long bookingId = fixture.createPassBooking(fixture.createFutureSlot().getId());

        fixture.mockMvc().perform(post("/admin/bookings/{id}/no-show", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("NO_SHOW"));
    }

    @DisplayName("8회권 전체 환불은 취소 건수와 환불 크레딧 요약을 반환한다")
    @Test
    void refundPass_returnsRefundSummary() throws Exception {
        fixture.createPassBooking(fixture.createFutureSlot().getId());
        fixture.createPassBooking(fixture.createFutureSlot(BookingTestHelper.FUTURE.plusDays(1)).getId());

        fixture.mockMvc().perform(post("/admin/passes/{passId}/refund", fixture.pass().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledBookings").value(2))
                .andExpect(jsonPath("$.refundCredits").value(6))
                .andExpect(jsonPath("$.refundAmount").value(240_000));
    }

    @DisplayName("잔여 크레딧이 없으면 8회권 예약 시 422를 반환한다")
    @Test
    void bookWithPassNoCredits_returns422() throws Exception {
        fixture.pass().expire();
        passPurchaseStorePort.save(fixture.pass());

        fixture.mockMvc().perform(post("/api/v1/me/bookings")
                        .cookie(fixture.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slotId": %d,
                                  "passId": %d
                                }
                                """.formatted(fixture.createFutureSlot().getId(), fixture.pass().getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PASS_CREDIT_INSUFFICIENT"));
    }
}
