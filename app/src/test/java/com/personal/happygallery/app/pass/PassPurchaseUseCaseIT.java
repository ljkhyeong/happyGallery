package com.personal.happygallery.app.pass;

import com.jayway.jsonpath.JsonPath;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class PassPurchaseUseCaseIT {

    @Autowired WebApplicationContext context;
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

    MockMvc mockMvc;
    Guest guest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

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

    @Test
    void expiry_batch_expiredPass_remainingZero_expireLedgerCreated() {
        // 이미 만료된 pass 직접 생성 (expiresAt = 과거)
        PassPurchase expiredPass = passPurchaseRepository.save(
                new PassPurchase(guest, LocalDateTime.now().minusDays(1), 0L));

        int processed = passExpiryBatchService.expireAll();

        assertThat(processed).isEqualTo(1);

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

    @Test
    void expiry_batch_activePass_notTouched() {
        // 미래 만료 pass
        passPurchaseRepository.save(new PassPurchase(guest, LocalDateTime.now().plusDays(30), 0L));

        int processed = passExpiryBatchService.expireAll();

        assertThat(processed).isEqualTo(0);
        assertThat(passLedgerRepository.count()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Proof: 만료 7일 전 알림 대상 조회 — 6일 후 만료 pass 포함, 30일 후는 제외
    // -----------------------------------------------------------------------

    @Test
    void notification_query_returnsPassesExpiringWithin7Days() {
        // 6일 후 만료 → 알림 대상
        Guest guest2 = guestRepository.save(new Guest("이알림", "01088880002"));
        passPurchaseRepository.save(new PassPurchase(guest, LocalDateTime.now().plusDays(6), 0L));

        // 30일 후 만료 → 알림 대상 아님
        passPurchaseRepository.save(new PassPurchase(guest2, LocalDateTime.now().plusDays(30), 0L));

        var expiring = passExpiryBatchService.findExpiringWithin7Days();

        assertThat(expiring).hasSize(1);
        assertThat(expiring.get(0).getGuest().getId()).isEqualTo(guest.getId());
    }

    // -----------------------------------------------------------------------
    // Proof: 존재하지 않는 guestId → 404
    // -----------------------------------------------------------------------

    @Test
    void purchase_unknownGuest_returns404() throws Exception {
        mockMvc.perform(post("/passes/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"guestId\": 99999 }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
