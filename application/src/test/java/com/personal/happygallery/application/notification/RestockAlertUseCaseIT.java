package com.personal.happygallery.application.notification;

import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.product.RestockAlertScheduler;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.OptionGroupDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.OptionValueDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SaveProductCommand;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SelectionDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.VariantDefinition;
import com.personal.happygallery.application.product.port.in.RestockAlertUseCase;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.PhoneVerificationRequiredException;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.ProductOptionType;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.RestockAlertStatus;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UseCaseIT
class RestockAlertUseCaseIT {
    @Autowired RestockAlertUseCase alerts;
    @Autowired RestockAlertScheduler scheduler;
    @Autowired ProductAdminUseCase products;
    @Autowired UserStorePort users;
    @Autowired NotificationOutboxRepository outboxes;
    @Autowired NotificationOutboxTransactionService transactions;
    @Autowired NotificationOutboxDispatcher dispatcher;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestCleanupSupport cleanup;
    @MockitoBean(name = "notificationExecutor") Executor executor;

    @AfterEach
    void cleanup() {
        cleanup.clearNotificationLogs();
        cleanup.clearOrderData();
        cleanup.clearUsers();
    }

    @Test
    @DisplayName("재입고 신청은 휴대폰 확인과 소유권을 검사하며 같은 상품의 중복 신청을 하나로 합친다")
    void register_enforcesVerifiedOwnerAndUniqueSubscription() {
        var product = readyStock();
        var user = users.save(new User("restock@example.com", "hash", "회원", "01012345678"));
        assertThatThrownBy(() -> alerts.register(user.getId(), product, null))
                .isInstanceOf(PhoneVerificationRequiredException.class);
        user.markPhoneVerified();
        users.save(user);
        var first = alerts.register(user.getId(), product, null);
        assertThat(alerts.register(user.getId(), product, null).getId()).isEqualTo(first.getId());
        assertThatThrownBy(() -> alerts.cancel(Long.MAX_VALUE, first.getId())).isInstanceOf(NotFoundException.class);
        alerts.cancel(user.getId(), first.getId());
        assertThat(alerts.register(user.getId(), product, null).getId()).isNotEqualTo(first.getId());
    }

    @Test
    @DisplayName("다른 옵션의 입고는 발송하지 않고 신청을 해지하면 대기 중 알림도 보내지 않는다")
    void restock_targetsVariantAndChecksCancellationBeforeDelivery() {
        var user = verifiedUser();
        var product = products.register(new SaveProductCommand("색상 상품", ProductType.MADE_TO_ORDER,
                null, 10000L, 0, null, null, "가죽", null, 3,
                List.of(new OptionGroupDefinition("color", ProductOptionType.SELECT, "색상", true, 0,
                        null, null, null, List.of(new OptionValueDefinition("red", "빨강", 0),
                        new OptionValueDefinition("blue", "파랑", 1)))),
                List.of(new VariantDefinition(List.of(new SelectionDefinition("color", "red")), 0, 0, true),
                        new VariantDefinition(List.of(new SelectionDefinition("color", "blue")), 0, 0, true))));
        long red = product.options().variants().stream().filter(v -> v.selections().getFirst().valueKey().equals("red")).findFirst().orElseThrow().id();
        long blue = product.options().variants().stream().filter(v -> v.id() != red).findFirst().orElseThrow().id();
        var alert = alerts.register(user.getId(), product.product().getId(), red);
        assertThat(alert.getOptionLabel()).isEqualTo("색상: 빨강");
        jdbc.update("UPDATE product_variants SET quantity = 1 WHERE id = ?", blue);
        assertThat(scheduler.processPending().successCount()).isZero();
        jdbc.update("UPDATE product_variants SET quantity = 1 WHERE id = ?", red);
        assertThat(scheduler.processPending().successCount()).isOne();
        assertThat(scheduler.processPending().successCount()).isZero();
        alerts.cancel(user.getId(), alert.getId());
        dispatcher.dispatchPending();
        assertThat(outboxes.findAll()).singleElement().satisfies(outbox ->
                assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.OBSOLETE));
        assertThat(scheduler.processPending().successCount()).isZero();
    }

    @Test
    @DisplayName("발송 전에 다시 품절되면 다음 입고에 같은 알림을 재사용하고 발송 완료 후에는 반복하지 않는다")
    void obsoleteRestock_reusesOutboxAndFinishesOnce() {
        var user = verifiedUser();
        var product = readyStock();
        var alert = alerts.register(user.getId(), product, null);
        jdbc.update("UPDATE inventory SET quantity = 1 WHERE product_id = ?", product);
        scheduler.processPending();
        Long outboxId = outboxes.findAll().getFirst().getId();
        jdbc.update("UPDATE inventory SET quantity = 0 WHERE product_id = ?", product);
        var reservation = transactions.reserveNextDispatchable(5).orElseThrow();
        assertThat(transactions.prepareDelivery(reservation.outboxId(), reservation.processingToken()).status())
                .isEqualTo(NotificationOutboxPreparationStatus.OBSOLETE);
        jdbc.update("UPDATE inventory SET quantity = 1 WHERE product_id = ?", product);
        assertThat(scheduler.processPending().successCount()).isOne();
        assertThat(outboxes.findAll()).singleElement().satisfies(outbox -> assertThat(outbox.getId()).isEqualTo(outboxId));
        dispatcher.dispatchPending();
        assertThat(outboxes.findById(outboxId).orElseThrow().getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
        scheduler.processPending();
        assertThat(alerts.list(user.getId())).singleElement().satisfies(view -> {
            assertThat(view.alert().getId()).isEqualTo(alert.getId());
            assertThat(view.alert().getStatus()).isEqualTo(RestockAlertStatus.NOTIFIED);
        });
        assertThat(scheduler.processPending().successCount()).isZero();
    }

    @Test
    @DisplayName("판매 중지 상품과 탈퇴 회원에게는 재입고 알림을 접수하지 않는다")
    void inactiveProductOrUser_doesNotReceiveAlert() {
        var user = verifiedUser();
        var product = readyStock();
        alerts.register(user.getId(), product, null);
        jdbc.update("UPDATE inventory SET quantity = 1 WHERE product_id = ?", product);
        jdbc.update("UPDATE products SET status = 'INACTIVE' WHERE id = ?", product);
        assertThat(scheduler.processPending().successCount()).isZero();
        jdbc.update("UPDATE products SET status = 'ACTIVE' WHERE id = ?", product);
        jdbc.update("UPDATE users SET withdrawn_at = CURRENT_TIMESTAMP WHERE id = ?", user.getId());
        assertThat(scheduler.processPending().successCount()).isZero();
    }

    private User verifiedUser() {
        var user = new User("restock@example.com", "hash", "회원", "01012345678");
        user.markPhoneVerified();
        return users.save(user);
    }

    private Long readyStock() {
        Long id = products.register(new SaveProductCommand("품절 상품", ProductType.READY_STOCK, null,
                10000, 1, null, null, null, null, null, List.of(), List.of())).product().getId();
        products.adjustInventory(new ProductAdminUseCase.AdjustInventoryCommand(id, null,
                InventoryAdjustmentType.DECREASE, 1, "판매 완료", null, "테스트 관리자"));
        return id;
    }
}
