package com.personal.happygallery.application.notification;

import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.NotificationLogProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@UseCaseIT
class NotificationOutboxUseCaseIT {

    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired NotificationOutboxRepository outboxRepository;
    @Autowired NotificationOutboxDispatcher outboxDispatcher;
    @Autowired UserStorePort userStorePort;
    @Autowired NotificationLogProbe notificationLogProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
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
                    assertThat(outboxes.get(0).getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
                });
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

    @DisplayName("알림 outbox dispatch는 활성 트랜잭션 안에서 실행하지 않는다")
    @Test
    void dispatchPending_insideTransaction_throwsException() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                outboxDispatcher.dispatchPending()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
