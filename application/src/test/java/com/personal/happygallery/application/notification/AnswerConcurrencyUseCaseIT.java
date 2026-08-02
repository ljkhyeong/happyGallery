package com.personal.happygallery.application.notification;

import com.personal.happygallery.adapter.out.persistence.inquiry.InquiryRepository;
import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.adapter.out.persistence.qna.ProductQnaRepository;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.inquiry.Inquiry;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.qna.ProductQna;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class AnswerConcurrencyUseCaseIT {

    private static final String FIRST_REPLY = "첫 번째 답변";
    private static final String SECOND_REPLY = "두 번째 답변";
    private static final long FIRST_ADMIN_ID = 101L;
    private static final long SECOND_ADMIN_ID = 202L;

    @Autowired InquiryUseCase inquiryUseCase;
    @Autowired ProductQnaUseCase productQnaUseCase;
    @Autowired UserStorePort userStore;
    @Autowired InquiryRepository inquiryRepository;
    @Autowired ProductQnaRepository productQnaRepository;
    @Autowired ProductRepository productRepository;
    @Autowired NotificationOutboxRepository outboxRepository;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @DisplayName("같은 문의에 답변이 동시에 요청되어도 한 건만 등록된다")
    @Test
    void concurrentInquiryReplies_registerOnlyOneAnswer() throws Exception {
        User user = createUser();
        Inquiry inquiry = inquiryRepository.save(
                new Inquiry(user.getId(), "배송 문의", "언제 배송되나요?"));

        ConcurrentResult result = executeSimultaneously(
                () -> inquiryUseCase.replyAndGet(
                        inquiry.getId(), FIRST_REPLY, FIRST_ADMIN_ID),
                () -> inquiryUseCase.replyAndGet(
                        inquiry.getId(), SECOND_REPLY, SECOND_ADMIN_ID));

        Inquiry persisted = inquiryRepository.findById(inquiry.getId()).orElseThrow();
        assertSingleAnswer(result, persisted.getReplyContent(), persisted.getRepliedBy(),
                "이미 답변이 등록된 문의입니다.");
    }

    @DisplayName("같은 상품 Q&A에 답변이 동시에 요청되어도 한 건만 등록된다")
    @Test
    void concurrentProductQnaReplies_registerOnlyOneAnswer() throws Exception {
        User user = createUser();
        Product product = productRepository.save(
                new Product("도자기 화병", ProductType.READY_STOCK, 30_000L));
        ProductQna qna = productQnaRepository.save(new ProductQna(
                product.getId(), user.getId(), "크기 문의", "높이가 몇 cm인가요?", false));

        ConcurrentResult result = executeSimultaneously(
                () -> productQnaUseCase.replyAndGet(
                        qna.getId(), FIRST_REPLY, FIRST_ADMIN_ID),
                () -> productQnaUseCase.replyAndGet(
                        qna.getId(), SECOND_REPLY, SECOND_ADMIN_ID));

        ProductQna persisted = productQnaRepository.findById(qna.getId()).orElseThrow();
        assertSingleAnswer(result, persisted.getReplyContent(), persisted.getRepliedBy(),
                "이미 답변이 등록된 Q&A입니다.");
    }

    private User createUser() {
        return userStore.save(new User(
                "answer-concurrency@example.com", "password-hash", "답변 대상 회원", "01012345678"));
    }

    private ConcurrentResult executeSimultaneously(Runnable firstCommand,
                                                   Runnable secondCommand) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Throwable> first = executor.submit(
                    () -> executeReply(ready, start, firstCommand));
            Future<Throwable> second = executor.submit(
                    () -> executeReply(ready, start, secondCommand));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return new ConcurrentResult(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private Throwable executeReply(CountDownLatch ready, CountDownLatch start, Runnable command) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new AssertionError("동시 답변 시작 신호를 기다리지 못했습니다.");
            }
            command.run();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private void assertSingleAnswer(ConcurrentResult result,
                                    String persistedReply,
                                    Long persistedAdminId,
                                    String duplicateMessage) {
        Throwable failure = Stream.of(result.firstFailure(), result.secondFailure())
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow();
        long successCount = Stream.of(result.firstFailure(), result.secondFailure())
                .filter(Objects::isNull)
                .count();
        Long expectedAdminId = FIRST_REPLY.equals(persistedReply)
                ? FIRST_ADMIN_ID
                : SECOND_ADMIN_ID;

        assertSoftly(softly -> {
            softly.assertThat(successCount).isEqualTo(1L);
            softly.assertThat(failure)
                    .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                            softly.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT))
                    .hasMessage(duplicateMessage);
            softly.assertThat(persistedReply).isIn(FIRST_REPLY, SECOND_REPLY);
            softly.assertThat(persistedAdminId).isEqualTo(expectedAdminId);
            softly.assertThat(outboxRepository.count()).isEqualTo(1L);
        });
    }

    private void cleanup() {
        productQnaRepository.deleteAllInBatch();
        inquiryRepository.deleteAllInBatch();
        cleanupSupport.clearNotificationLogs();
        cleanupSupport.clearProductData();
        cleanupSupport.clearUsers();
    }

    private record ConcurrentResult(Throwable firstFailure, Throwable secondFailure) {
    }
}
