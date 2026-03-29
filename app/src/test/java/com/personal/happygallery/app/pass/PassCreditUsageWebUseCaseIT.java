package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class PassCreditUsageWebUseCaseIT extends PassCreditUsageTestSupport {

    @org.springframework.beans.factory.annotation.Autowired
    PassPurchaseStorePort passPurchaseStorePort;

    @DisplayName("8회권으로 예약하면 BOOKED 응답을 반환한다")
    @Test
    void bookWithPass_returnsBookedResponse() throws Exception {
        var slot = createFutureSlot();

        mockMvc.perform(post("/api/v1/me/bookings")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slotId": %d,
                                  "passId": %d
                                }
                                """.formatted(slot.getId(), pass.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @DisplayName("8회권 예약을 기한 내 취소하면 refundable=true를 반환한다")
    @Test
    void cancelPassBookingTimely_returnsRefundableTrue() throws Exception {
        Long bookingId = createPassBooking(createFutureSlot().getId());

        mockMvc.perform(delete("/api/v1/me/bookings/{id}", bookingId)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true));
    }

    @DisplayName("8회권 예약을 늦게 취소하면 refundable=false를 반환한다")
    @Test
    void cancelPassBookingLate_returnsRefundableFalse() throws Exception {
        LocalDateTime today14 = LocalDateTime.now(clock).toLocalDate().atTime(14, 0);
        Long bookingId = createPassBooking(createFutureSlot(today14).getId());

        mockMvc.perform(delete("/api/v1/me/bookings/{id}", bookingId)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(false));
    }

    @DisplayName("노쇼 처리 요청은 NO_SHOW 응답을 반환한다")
    @Test
    void markNoShow_returnsNoShowResponse() throws Exception {
        Long bookingId = createPassBooking(createFutureSlot().getId());

        mockMvc.perform(post("/admin/bookings/{id}/no-show", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("NO_SHOW"));
    }

    @DisplayName("8회권 전체 환불은 취소 건수와 환불 크레딧 요약을 반환한다")
    @Test
    void refundPass_returnsRefundSummary() throws Exception {
        createPassBooking(createFutureSlot().getId());
        createPassBooking(createFutureSlot(BookingTestHelper.FUTURE.plusDays(1)).getId());

        mockMvc.perform(post("/admin/passes/{passId}/refund", pass.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledBookings").value(2))
                .andExpect(jsonPath("$.refundCredits").value(6))
                .andExpect(jsonPath("$.refundAmount").value(240_000));
    }

    @DisplayName("잔여 크레딧이 없으면 8회권 예약 시 422를 반환한다")
    @Test
    void bookWithPassNoCredits_returns422() throws Exception {
        pass.expire();
        passPurchaseStorePort.save(pass);

        mockMvc.perform(post("/api/v1/me/bookings")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slotId": %d,
                                  "passId": %d
                                }
                                """.formatted(createFutureSlot().getId(), pass.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PASS_CREDIT_INSUFFICIENT"));
    }
}
