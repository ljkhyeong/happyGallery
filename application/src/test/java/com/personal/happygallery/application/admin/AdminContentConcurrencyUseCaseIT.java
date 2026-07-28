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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class AdminContentConcurrencyUseCaseIT {

    @Autowired NoticeAdminUseCase noticeAdminUseCase;
    @Autowired NoticeQueryUseCase noticeQueryUseCase;
    @Autowired NoticeRepository noticeRepository;
    @Autowired WorkshopProfileUseCase workshopProfileUseCase;

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
}
