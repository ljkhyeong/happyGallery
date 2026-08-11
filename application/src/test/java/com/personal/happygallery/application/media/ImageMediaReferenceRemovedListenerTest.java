package com.personal.happygallery.application.media;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageMediaReferenceRemovedListenerTest {

    @Test
    @DisplayName("커밋된 참조 제거 이벤트는 URL을 같은 로컬 파일명으로 해석해 참조 재확인 삭제를 호출한다")
    void deleteLocalFileWithReferenceRecheck() {
        ImageMediaDeletionTransactionService deletionTransaction =
                mock(ImageMediaDeletionTransactionService.class);
        ImageMediaReferenceRemovedListener listener =
                new ImageMediaReferenceRemovedListener(deletionTransaction);

        listener.deleteAfterCommit(new ImageMediaReferenceRemovedEvent(List.of(
                "/api/v1/media/images/preserved%2Ejpg?v=1#preview",
                "/api/v1/media/images/second.png",
                "/api/v1/media/images/second.png")));

        verify(deletionTransaction).deleteIfUnreferenced(
                Set.of("preserved.jpg", "second.png"));
    }

    @Test
    @DisplayName("커밋 후 파일 삭제가 실패해도 이미 성공한 참조 제거 요청은 실패로 바꾸지 않는다")
    void leaveFailedDeletionForRetentionFallback() {
        ImageMediaDeletionTransactionService deletionTransaction =
                mock(ImageMediaDeletionTransactionService.class);
        ImageMediaReferenceRemovedListener listener =
                new ImageMediaReferenceRemovedListener(deletionTransaction);
        when(deletionTransaction.deleteIfUnreferenced(Set.of("failed.png")))
                .thenThrow(new IllegalStateException("storage unavailable"));

        assertThatCode(() -> listener.deleteAfterCommit(new ImageMediaReferenceRemovedEvent(
                List.of("/api/v1/media/images/failed.png"))))
                .doesNotThrowAnyException();
    }
}
