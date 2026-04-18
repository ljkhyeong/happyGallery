package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.application.pass.port.in.PassPurchaseUseCase;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.pass.port.out.PassLedgerReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static com.personal.happygallery.support.TestFixtures.passPurchase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class PassPurchaseUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserStorePort userStorePort;
    @Autowired PassPurchaseStorePort passPurchaseStorePort;
    @Autowired PassPurchaseReaderPort passPurchaseReaderPort;
    @Autowired PassLedgerReaderPort passLedgerReaderPort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired PassExpiryBatchUseCase passExpiryBatchService;
    @Autowired PassPurchaseUseCase passPurchaseUseCase;
    @Autowired Clock clock;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    // -----------------------------------------------------------------------
    // Proof: 구매 성공 → remaining=8, EARN ledger 1건 생성
    // -----------------------------------------------------------------------

    @DisplayName("회원 8회권 구매 성공 시 잔여 크레딧 8과 EARN 원장이 생성된다")
    @Test
    void purchase_success_remainingCredits8_earnLedgerCreated() {
        User user = userStorePort.save(new User("pass@example.com", "hashed-password", "회원", "01012345678"));
        PassPurchase purchased = passPurchaseUseCase.purchaseForMember(user.getId(), 120_000L);
        Long passId = purchased.getId();

        // Proof: EARN ledger 1건, amount=8
        var ledgers = passLedgerReaderPort.findByPassPurchaseId(passId);
        assertSoftly(softly -> {
            softly.assertThat(purchased.getUserId()).isEqualTo(user.getId());
            softly.assertThat(purchased.getRemainingCredits()).isEqualTo(8);
            softly.assertThat(ledgers).hasSize(1);
            softly.assertThat(ledgers.get(0).getType()).isEqualTo(PassLedgerType.EARN);
            softly.assertThat(ledgers.get(0).getAmount()).isEqualTo(8);
        });
    }

    // -----------------------------------------------------------------------
    // Proof: 만료 배치 — remaining_credits=0, EXPIRE ledger 기록
    // -----------------------------------------------------------------------

    @DisplayName("만료된 8회권은 잔여 크레딧이 0이 되고 EXPIRE 원장이 생성된다")
    @Test
    void expiry_batch_expiredPass_remainingZero_expireLedgerCreated() {
        User user = userStorePort.save(new User("expired-pass@example.com", "hashed-password", "회원", "01011112222"));
        // 이미 만료된 pass 직접 생성 (expiresAt = 과거)
        PassPurchase expiredPass = passPurchaseStorePort.save(
                passPurchase(user.getId(), LocalDateTime.now(clock).minusDays(1), 0L));

        BatchResult result = passExpiryBatchService.expireAll();

        // Proof: remaining_credits = 0
        PassPurchase reloaded = passPurchaseReaderPort.findById(expiredPass.getId()).orElseThrow();

        // Proof: EARN(구매 직접 저장 시 없음) + EXPIRE ledger 1건
        var ledgers = passLedgerReaderPort.findByPassPurchaseId(expiredPass.getId());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(0);
            softly.assertThat(ledgers).hasSize(1);
            softly.assertThat(ledgers.get(0).getType()).isEqualTo(PassLedgerType.EXPIRE);
            softly.assertThat(ledgers.get(0).getAmount()).isEqualTo(8);
        });
    }

    // -----------------------------------------------------------------------
    // Proof: 만료 배치 — 아직 유효한 pass는 스킵
    // -----------------------------------------------------------------------

    @DisplayName("유효한 8회권은 만료 배치에서 변경되지 않는다")
    @Test
    void expiry_batch_activePass_notTouched() {
        User user = userStorePort.save(new User("active-pass@example.com", "hashed-password", "회원", "01022223333"));
        // 미래 만료 pass
        PassPurchase activePass = passPurchaseStorePort.save(
                passPurchase(user.getId(), LocalDateTime.now(clock).plusDays(30), 0L));

        BatchResult result = passExpiryBatchService.expireAll();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(0);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(passLedgerReaderPort.findByPassPurchaseId(activePass.getId())).isEmpty();
        });
    }

    @DisplayName("8회권 만료 배치 관리자 API는 배치 결과를 반환한다")
    @Test
    void expiry_batch_adminApi_returnsBatchResponse() throws Exception {
        User user = userStorePort.save(new User("admin-pass@example.com", "hashed-password", "회원", "01033334444"));
        passPurchaseStorePort.save(passPurchase(user.getId(), LocalDateTime.now(clock).minusDays(1), 0L));

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
        User firstUser = userStorePort.save(new User("notify-pass-1@example.com", "hashed-password", "회원", "01044445555"));
        User secondUser = userStorePort.save(new User("notify-pass-2@example.com", "hashed-password", "회원", "01055556666"));
        passPurchaseStorePort.save(passPurchase(firstUser.getId(), LocalDateTime.now(clock).plusDays(7), 0L));

        // 30일 후 만료 → 알림 대상 아님
        passPurchaseStorePort.save(passPurchase(secondUser.getId(), LocalDateTime.now(clock).plusDays(30), 0L));

        var expiring = passExpiryBatchService.findExpiringWithin7Days();

        assertSoftly(softly -> {
            softly.assertThat(expiring).hasSize(1);
            softly.assertThat(expiring.get(0).getUserId()).isEqualTo(firstUser.getId());
        });
    }

    // -----------------------------------------------------------------------
    // Proof: 존재하지 않는 guestId → 404
    // -----------------------------------------------------------------------

    @DisplayName("만료 임박 조회는 회원 소유 8회권만 반환한다")
    @Test
    void notification_query_returnsOnlyMemberOwnedPasses() {
        User firstMember = userStorePort.save(new User("member-1@example.com", "hashed-password", "회원", "01066667777"));
        User secondMember = userStorePort.save(new User("member-2@example.com", "hashed-password", "회원", "01077778888"));
        PassPurchase firstPass = passPurchaseStorePort.save(
                PassPurchase.forMember(firstMember.getId(), LocalDateTime.now(clock).plusDays(7), 120_000L));
        PassPurchase secondPass = passPurchaseStorePort.save(
                passPurchase(secondMember.getId(), LocalDateTime.now(clock).plusDays(7), 120_000L));

        var expiring = passExpiryBatchService.findExpiringWithin7Days();

        assertThat(expiring)
                .extracting(PassPurchase::getId)
                .contains(firstPass.getId(), secondPass.getId());
    }
}
