package com.personal.happygallery.application.notification;

import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.adapter.out.persistence.notification.NotificationLogRepository;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.NotificationLogProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

@UseCaseIT
class NotificationOutboxUseCaseIT {

    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired NotificationOutboxRepository outboxRepository;
    @Autowired NotificationOutboxDispatcher outboxDispatcher;
    @Autowired NotificationOutboxService outboxService;
    @Autowired NotificationOutboxTransactionService outboxTransactionService;
    @Autowired NotificationQueryUseCase notificationQueryUseCase;
    @Autowired NotificationLogRepository notificationLogRepository;
    @Autowired UserStorePort userStorePort;
    @Autowired NotificationLogProbe notificationLogProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearNotificationLogs();
        cleanupSupport.clearUsers();
    }

    @DisplayName("알림 이벤트는 outbox에 저장되고 커밋 이후 발송된다")
    @Test
    void notificationEvent_enqueuesOutboxAndDispatchesAfterCommit() {
        User user = userStorePort.save(new User("outbox@example.com", "hash", "회원", "01012345678"));

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                        user.getId(),
                        NotificationEventType.PASS_EXPIRY_SOON,
                        "PASS_PURCHASE",
                        1L)));

        awaitLogCount(notificationLogProbe, 1);
        await().atMost(2, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var outboxes = outboxRepository.findAll();
                    assertThat(outboxes).hasSize(1);
                    assertThat(outboxes.getFirst().getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
                });
    }

    @DisplayName("회원 알림함은 채널별 감사 로그가 아니라 발송된 논리 이벤트를 한 건으로 조회한다")
    @Test
    void memberInbox_readsSentOutboxInsteadOfChannelAuditLogs() {
        User user = userStorePort.save(new User("inbox@example.com", "hash", "회원", "01055556666"));
        LocalDateTime now = LocalDateTime.now(clock);
        NotificationOutbox outbox = outboxRepository.save(NotificationOutbox.from(
                NotificationRequestedEvent.forUser(
                        user.getId(), NotificationEventType.ORDER_PAID, "ORDER", 10L),
                now));
        String token = outbox.markProcessing(now);
        outbox.markSent(token, now);
        outboxRepository.save(outbox);
        notificationLogRepository.save(NotificationLog.failed(
                null, user.getId(), NotificationChannel.KAKAO,
                NotificationEventType.ORDER_PAID, "PERMANENT_FAILURE", now));
        notificationLogRepository.save(NotificationLog.success(
                null, user.getId(), NotificationChannel.SMS,
                NotificationEventType.ORDER_PAID, now));

        var notifications = notificationQueryUseCase.listNotifications(user.getId(), null, 0, 20);

        assertSoftly(softly -> {
            softly.assertThat(notifications)
                    .singleElement()
                    .satisfies(notification -> {
                        softly.assertThat(notification.id()).isEqualTo(outbox.getId());
                        softly.assertThat(notification.eventType()).isEqualTo(NotificationEventType.ORDER_PAID);
                        softly.assertThat(notification.aggregateType()).isEqualTo("ORDER");
                        softly.assertThat(notification.aggregateId()).isEqualTo(10L);
                        softly.assertThat(notification.isRead()).isFalse();
                    });
            softly.assertThat(notificationQueryUseCase.countUnread(user.getId(), null)).isOne();
        });

        notificationQueryUseCase.markAsRead(outbox.getId(), user.getId(), null);

        assertThat(notificationQueryUseCase.countUnread(user.getId(), null)).isZero();
    }

    @DisplayName("알림 이벤트가 발행된 트랜잭션이 롤백되면 outbox도 생성되지 않는다")
    @Test
    void notificationEvent_rollsBackWithPublisherTransaction() {
        User user = userStorePort.save(new User("outbox-rollback@example.com", "hash", "회원", "01087654321"));

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                    user.getId(),
                    NotificationEventType.PASS_EXPIRY_SOON,
                    "PASS_PURCHASE",
                    2L));
            throw new RuntimeException("rollback");
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("rollback");

        await().during(300, TimeUnit.MILLISECONDS)
                .atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(outboxRepository.findAll()).isEmpty();
                    assertThat(notificationLogProbe.all()).isEmpty();
                });
    }

    @DisplayName("같은 멱등키의 동시 요청은 DB 유일 제약으로 outbox 한 건만 저장한다")
    @Test
    void enqueue_sameIdempotencyKeyConcurrently_insertsOnce() throws Exception {
        User user = userStorePort.save(new User(
                "outbox-concurrent@example.com", "hash", "회원", "01022223333"));
        NotificationRequestedEvent event = NotificationRequestedEvent.forUser(
                user.getId(), NotificationEventType.PASS_EXPIRY_SOON, "PASS_PURCHASE", 20L);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> enqueue = () -> {
            ready.countDown();
            if (!start.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 enqueue 시작 대기 시간 초과");
            }
            return Boolean.TRUE.equals(new TransactionTemplate(transactionManager)
                    .execute(status -> outboxService.enqueue(event)));
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(enqueue);
            Future<Boolean> second = executor.submit(enqueue);
            assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Boolean> results = List.of(
                    first.get(5, TimeUnit.SECONDS),
                    second.get(5, TimeUnit.SECONDS));

            assertSoftly(softly -> {
                softly.assertThat(results).containsExactlyInAnyOrder(true, false);
                softly.assertThat(outboxRepository.findAll())
                        .singleElement()
                        .satisfies(outbox -> softly.assertThat(outbox.getIdempotencyKey())
                                .isEqualTo(event.idempotencyKey()));
            });
        }
    }

    @DisplayName("알림 outbox dispatch는 활성 트랜잭션 안에서 실행하지 않는다")
    @Test
    void dispatchPending_insideTransaction_throwsException() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                outboxDispatcher.dispatchPending()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @DisplayName("알림 처리 중 예외도 재시도 횟수와 실패 사유에 기록된다")
    @Test
    void dispatchPending_deliveryExceptionRecordsFailure() {
        User user = userStorePort.save(new User(
                "outbox-exception@example.com", "hash", "회원", "01011112222"));
        NotificationOutbox outbox = outboxRepository.save(NotificationOutbox.from(
                NotificationRequestedEvent.forUser(
                        user.getId(), NotificationEventType.PASS_EXPIRY_SOON, "PASS_PURCHASE", 3L),
                LocalDateTime.now(clock)));
        jdbcTemplate.update("UPDATE users SET phone_enc = 'invalid' WHERE id = ?", user.getId());

        outboxDispatcher.dispatchPending();

        NotificationOutbox failed = outboxRepository.findById(outbox.getId()).orElseThrow();
        var backlog = outboxRepository.summarizeUnresolvedBacklog();
        assertSoftly(softly -> {
            softly.assertThat(failed.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
            softly.assertThat(failed.getAttemptCount()).isOne();
            softly.assertThat(failed.getLastError()).startsWith("DISPATCH_EXCEPTION:");
            softly.assertThat(backlog)
                    .singleElement()
                    .satisfies(summary -> {
                        softly.assertThat(summary.status()).isEqualTo(NotificationOutboxStatus.PENDING);
                        softly.assertThat(summary.count()).isOne();
                        softly.assertThat(summary.oldestActionAt()).isAfter(LocalDateTime.now(clock));
                    });
        });
    }

    @DisplayName("재선점 전 처리 토큰의 늦은 성공과 실패는 최신 outbox 상태를 덮지 않는다")
    @Test
    void staleProcessingToken_cannotOverwriteLatestClaim() {
        User user = userStorePort.save(new User(
                "outbox-fencing@example.com", "hash", "회원", "01033334444"));
        NotificationOutbox outbox = outboxRepository.save(NotificationOutbox.from(
                NotificationRequestedEvent.forUser(
                        user.getId(), NotificationEventType.PASS_EXPIRY_SOON, "PASS_PURCHASE", 4L),
                LocalDateTime.now(clock)));

        NotificationOutboxReservation first = outboxTransactionService
                .reserveDispatchable(1, 1)
                .getFirst();
        jdbcTemplate.update(
                "UPDATE notification_outbox SET locked_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusMinutes(2),
                outbox.getId());
        NotificationOutboxReservation second = outboxTransactionService
                .reserveDispatchable(1, 1)
                .getFirst();

        boolean staleSuccessAccepted = outboxTransactionService.markSent(
                first.outboxId(), first.processingToken());
        boolean staleFailureAccepted = outboxTransactionService.markDeliveryFailed(
                first.outboxId(), first.processingToken(), "LATE_FAILURE", 5);
        NotificationOutbox processing = outboxRepository.findById(outbox.getId()).orElseThrow();
        boolean latestSuccessAccepted = outboxTransactionService.markSent(
                second.outboxId(), second.processingToken());

        assertSoftly(softly -> {
            softly.assertThat(second.processingToken()).isNotEqualTo(first.processingToken());
            softly.assertThat(staleSuccessAccepted).isFalse();
            softly.assertThat(staleFailureAccepted).isFalse();
            softly.assertThat(processing.getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING);
            softly.assertThat(processing.getProcessingToken()).isEqualTo(second.processingToken());
            softly.assertThat(processing.getAttemptCount()).isZero();
            softly.assertThat(processing.getLastError()).isNull();
            softly.assertThat(latestSuccessAccepted).isTrue();
            softly.assertThat(outboxRepository.findById(outbox.getId()).orElseThrow().getStatus())
                    .isEqualTo(NotificationOutboxStatus.SENT);
        });
    }
}
