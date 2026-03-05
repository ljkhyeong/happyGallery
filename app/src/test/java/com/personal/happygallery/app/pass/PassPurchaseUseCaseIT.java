package com.personal.happygallery.app.pass;

import com.jayway.jsonpath.JsonPath;
import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.PhoneVerificationRepository;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class PassPurchaseUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired PassPurchaseRepository passPurchaseRepository;
    @Autowired PassLedgerRepository passLedgerRepository;
    @Autowired PassExpiryBatchService passExpiryBatchService;
    @Autowired GuestRepository guestRepository;
    @Autowired PhoneVerificationRepository phoneVerificationRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired ClassRepository classRepository;

    Guest guest;

    @BeforeEach
    void setUp() {
        // FK 순서: passLedger → refund → bookingHistory → booking(→ pass_purchases FK)
        //         → passPurchase → phoneVerification → guest → slot → class
        passLedgerRepository.deleteAll();
        refundRepository.deleteAll();
        bookingHistoryRepository.deleteAll();
        bookingRepository.deleteAll();
        passPurchaseRepository.deleteAll();
        phoneVerificationRepository.deleteAll();
        guestRepository.deleteAll();
        slotRepository.deleteAll();
        classRepository.deleteAll();

        guest = guestRepository.save(new Guest("김테스트", "01099990001"));
    }

    // -----------------------------------------------------------------------
    // Proof: 구매 성공 → remaining=8, EARN ledger 1건 생성
    // -----------------------------------------------------------------------

    @DisplayName("8회권 구매 성공 시 잔여 크레딧 8과 EARN 원장이 생성된다")
    @Test
    void purchase_success_remainingCredits8_earnLedgerCreated() throws Exception {
        String resp = mockMvc.perform(post("/passes/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "guestId": %d }
                                """.formatted(guest.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passId").isNumber())
                .andExpect(jsonPath("$.guestId").value(guest.getId()))
                .andExpect(jsonPath("$.totalCredits").value(8))
                .andExpect(jsonPath("$.remainingCredits").value(8))
                .andReturn().getResponse().getContentAsString();

        Long passId = ((Number) JsonPath.read(resp, "$.passId")).longValue();

        // Proof: EARN ledger 1건, amount=8
        var ledgers = passLedgerRepository.findByPassPurchaseId(passId);
        assertThat(ledgers).hasSize(1);
        assertThat(ledgers.get(0).getType()).isEqualTo(PassLedgerType.EARN);
        assertThat(ledgers.get(0).getAmount()).isEqualTo(8);
    }

    // -----------------------------------------------------------------------
    // Proof: 만료 배치 — remaining_credits=0, EXPIRE ledger 기록
    // -----------------------------------------------------------------------

    @DisplayName("만료된 8회권은 잔여 크레딧이 0이 되고 EXPIRE 원장이 생성된다")
    @Test
    void expiry_batch_expiredPass_remainingZero_expireLedgerCreated() {
        // 이미 만료된 pass 직접 생성 (expiresAt = 과거)
        PassPurchase expiredPass = passPurchaseRepository.save(
                new PassPurchase(guest, LocalDateTime.now().minusDays(1), 0L));

        BatchResult result = passExpiryBatchService.expireAll();

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();

        // Proof: remaining_credits = 0
        PassPurchase reloaded = passPurchaseRepository.findById(expiredPass.getId()).orElseThrow();
        assertThat(reloaded.getRemainingCredits()).isEqualTo(0);

        // Proof: EARN(구매 직접 저장 시 없음) + EXPIRE ledger 1건
        var ledgers = passLedgerRepository.findByPassPurchaseId(expiredPass.getId());
        assertThat(ledgers).hasSize(1);
        assertThat(ledgers.get(0).getType()).isEqualTo(PassLedgerType.EXPIRE);
        assertThat(ledgers.get(0).getAmount()).isEqualTo(8);
    }

    // -----------------------------------------------------------------------
    // Proof: 만료 배치 — 아직 유효한 pass는 스킵
    // -----------------------------------------------------------------------

    @DisplayName("유효한 8회권은 만료 배치에서 변경되지 않는다")
    @Test
    void expiry_batch_activePass_notTouched() {
        // 미래 만료 pass
        passPurchaseRepository.save(new PassPurchase(guest, LocalDateTime.now().plusDays(30), 0L));

        BatchResult result = passExpiryBatchService.expireAll();

        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failureCount()).isZero();
        assertThat(passLedgerRepository.count()).isEqualTo(0);
    }

    @DisplayName("8회권 만료 배치 관리자 API는 배치 결과를 반환한다")
    @Test
    void expiry_batch_adminApi_returnsBatchResponse() throws Exception {
        passPurchaseRepository.save(new PassPurchase(guest, LocalDateTime.now().minusDays(1), 0L));

        mockMvc.perform(post("/admin/passes/expire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(0))
                .andExpect(jsonPath("$.failureReasons").isMap());
    }

    // -----------------------------------------------------------------------
    // Proof: 만료 7일 전 알림 대상 조회 — 6일 후 만료 pass 포함, 30일 후는 제외
    // -----------------------------------------------------------------------

    @DisplayName("만료 알림 조회는 7일 이내 만료되는 8회권만 반환한다")
    @Test
    void notification_query_returnsPassesExpiringWithin7Days() {
        // 정확히 7일 후 만료 → 알림 대상
        Guest guest2 = guestRepository.save(new Guest("이알림", "01088880002"));
        passPurchaseRepository.save(new PassPurchase(guest, LocalDateTime.now().plusDays(7), 0L));

        // 30일 후 만료 → 알림 대상 아님
        passPurchaseRepository.save(new PassPurchase(guest2, LocalDateTime.now().plusDays(30), 0L));

        var expiring = passExpiryBatchService.findExpiringWithin7Days();

        assertThat(expiring).hasSize(1);
        assertThat(expiring.get(0).getGuest().getId()).isEqualTo(guest.getId());
    }

    // -----------------------------------------------------------------------
    // Proof: 존재하지 않는 guestId → 404
    // -----------------------------------------------------------------------

    @DisplayName("존재하지 않는 게스트가 8회권 구매를 요청하면 404를 반환한다")
    @Test
    void purchase_unknownGuest_returns404() throws Exception {
        mockMvc.perform(post("/passes/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"guestId\": 99999 }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
