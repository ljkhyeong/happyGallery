package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.OrderTestHelper;
import com.personal.happygallery.support.OrderStateProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [UseCaseIT] 단일 재고 동시 주문 동시성 검증.
 *
 * <p>Proof (§12.1 DoD): quantity=1 상품에 5개 스레드가 동시에 주문할 때
 * PESSIMISTIC_WRITE 락으로 정확히 1건만 성공하고 나머지는 재고 부족으로 실패한다.
 */
@UseCaseIT
class ConcurrentOrderUseCaseIT {

    @Autowired OrderStateProbe orderStateProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired OrderTestHelper orderHelper;

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    // -----------------------------------------------------------------------
    // Proof (§12.1 ★★★): quantity=1 → 동시 5건 중 1건만 성공
    // -----------------------------------------------------------------------

    @DisplayName("수량 1 재고에서 동시 주문을 시도하면 1건만 성공한다")
    @Test
    void concurrentOrder_quantity1_onlyOneSucceeds() throws InterruptedException {
        Product product = orderHelper.createReadyStockProduct("단일 작품(동시성)", 50000L, 1);
        User member = orderHelper.createMemberOwner();

        int threadCount = 5;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures  = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                try {
                    startLatch.await();
                    orderHelper.createMemberPickupOrder(member.getId(),
                            List.of(OrderTestHelper.item(product, 1, 50000L)));
                    successes.incrementAndGet();
                } catch (InventoryNotEnoughException e) {
                    failures.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        }

        startLatch.countDown();
        exec.shutdown();
        exec.awaitTermination(15, TimeUnit.SECONDS);

        // 최종 재고 0 확인 (음수로 내려가지 않음)
        int remaining = orderStateProbe.getInventoryByProductId(product.getId()).getQuantity();

        assertSoftly(softly -> {
            softly.assertThat(successes.get()).isEqualTo(1);
            softly.assertThat(failures.get()).isEqualTo(threadCount - 1);
            softly.assertThat(remaining).isEqualTo(0);
        });
    }
}
