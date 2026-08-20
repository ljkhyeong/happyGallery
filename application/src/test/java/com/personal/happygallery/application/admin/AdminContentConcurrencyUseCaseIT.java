package com.personal.happygallery.application.admin;

import com.personal.happygallery.adapter.out.persistence.notice.NoticeRepository;
import com.personal.happygallery.application.notice.port.in.NoticeAdminUseCase;
import com.personal.happygallery.application.notice.port.in.NoticeQueryUseCase;
import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase.UpdateCommand;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.notice.Notice;
import com.personal.happygallery.domain.store.WorkshopProfile;
import com.personal.happygallery.support.UseCaseIT;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class AdminContentConcurrencyUseCaseIT {

    @Autowired NoticeAdminUseCase noticeAdminUseCase;
    @Autowired NoticeQueryUseCase noticeQueryUseCase;
    @Autowired NoticeRepository noticeRepository;
    @Autowired WorkshopProfileUseCase workshopProfileUseCase;
    @Autowired PlatformTransactionManager transactionManager;

    private WorkshopProfile originalWorkshopProfile;

    @AfterEach
    void tearDown() {
        noticeRepository.deleteAllInBatch();
        if (originalWorkshopProfile != null) {
            WorkshopProfile current = workshopProfileUseCase.get();
            workshopProfileUseCase.update(commandFrom(
                    originalWorkshopProfile,
                    originalWorkshopProfile.getName(),
                    current.getVersion()));
        }
    }

    @DisplayName("동시 공지 조회와 관리자 수정은 서로 덮지 않고 오래된 편집 요청은 충돌한다")
    @Test
    void concurrentNoticeViews_andAdminUpdates_preserveCommittedChanges() throws Exception {
        Notice adminSnapshot = noticeAdminUseCase.create("동시성 공지", "최초 본문", false);
        long expectedVersion = adminSnapshot.getVersion();

        int viewCount = 12;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(viewCount)) {
            for (int i = 0; i < viewCount; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    noticeQueryUseCase.getDetail(adminSnapshot.getId());
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        }

        noticeAdminUseCase.update(
                adminSnapshot.getId(),
                expectedVersion,
                "먼저 반영된 제목",
                "먼저 반영된 본문",
                false);

        assertThatThrownBy(() -> noticeAdminUseCase.update(
                adminSnapshot.getId(),
                expectedVersion,
                "뒤늦은 제목",
                "뒤늦은 본문",
                true))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        assertThatThrownBy(() -> noticeAdminUseCase.delete(
                adminSnapshot.getId(), expectedVersion))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

        Notice persisted = noticeRepository.findById(adminSnapshot.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(persisted.getViewCount()).isEqualTo(viewCount);
            softly.assertThat(persisted.getTitle()).isEqualTo("먼저 반영된 제목");
            softly.assertThat(persisted.getContent()).isEqualTo("먼저 반영된 본문");
            softly.assertThat(persisted.isPinned()).isFalse();
        });
    }

    @DisplayName("같은 공지 버전을 읽은 두 트랜잭션 중 하나만 저장된다")
    @Test
    void concurrentNoticeUpdates_withSameVersion_allowsOneWriter() throws Exception {
        Notice notice = noticeAdminUseCase.create("동시 수정 공지", "최초 본문", false);
        CountDownLatch loaded = new CountDownLatch(2);
        CountDownLatch update = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Throwable> first = executor.submit(
                    () -> updateNoticeAfterBothRead(notice.getId(), "첫 번째 제목", loaded, update));
            Future<Throwable> second = executor.submit(
                    () -> updateNoticeAfterBothRead(notice.getId(), "두 번째 제목", loaded, update));

            assertThat(loaded.await(5, TimeUnit.SECONDS)).isTrue();
            update.countDown();
            List<Throwable> results = new ArrayList<>(2);
            results.add(first.get(10, TimeUnit.SECONDS));
            results.add(second.get(10, TimeUnit.SECONDS));
            Throwable failure = results.stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow();
            Notice persisted = noticeRepository.findById(notice.getId()).orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(results.stream().filter(Objects::isNull).count())
                        .isEqualTo(1);
                softly.assertThat(failure)
                        .isInstanceOf(OptimisticLockingFailureException.class);
                softly.assertThat(persisted.getTitle())
                        .isIn("첫 번째 제목", "두 번째 제목");
                softly.assertThat(persisted.getVersion())
                        .isEqualTo(notice.getVersion() + 1);
            });
        }
    }

    @DisplayName("먼저 읽은 공방 정보는 다른 관리자의 수정 결과를 덮어쓰지 못한다")
    @Test
    void updateWorkshopProfile_withStaleVersion_throwsConflict() {
        WorkshopProfile staleProfile = workshopProfileUseCase.get();
        originalWorkshopProfile = staleProfile;
        long expectedVersion = staleProfile.getVersion();

        workshopProfileUseCase.update(commandFrom(
                staleProfile,
                "먼저 반영된 공방명",
                expectedVersion));

        assertThatThrownBy(() -> workshopProfileUseCase.update(commandFrom(
                staleProfile,
                "뒤늦게 저장한 공방명",
                expectedVersion)))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        assertThat(workshopProfileUseCase.get().getName())
                .isEqualTo("먼저 반영된 공방명");
    }

    private static UpdateCommand commandFrom(
            WorkshopProfile profile,
            String name,
            long expectedVersion
    ) {
        return new UpdateCommand(
                expectedVersion,
                name, profile.getPhone(), profile.getPostalCode(),
                profile.getAddressLine1(), profile.getAddressLine2(), profile.getBusinessHours(),
                profile.getMapUrl(), profile.getParkingInfo(), profile.getBusinessRegistrationNumber(),
                profile.getRepresentativeName(), profile.getEmail(), profile.getMailOrderRegistrationNumber(),
                profile.getIntroduction(), profile.getKakaoTalkId(),
                profile.getNaverTalkUrl(), profile.getNaverBlogUrl(),
                profile.getInstagramUrl(), profile.getSmartStoreUrl());
    }

    private Throwable updateNoticeAfterBothRead(
            Long noticeId,
            String title,
            CountDownLatch loaded,
            CountDownLatch update
    ) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                Notice notice = noticeRepository.findById(noticeId).orElseThrow();
                loaded.countDown();
                await(update);
                notice.update(title, "동시 수정 본문", false);
                noticeRepository.flush();
            });
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 수정 시작 신호를 기다리지 못했습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 수정 대기가 중단되었습니다.", exception);
        }
    }
}
