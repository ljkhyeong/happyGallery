package com.personal.happygallery.application.media;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class ImageMediaReferenceRemovedListener {

    private static final Logger log =
            LoggerFactory.getLogger(ImageMediaReferenceRemovedListener.class);

    private final ImageMediaDeletionTransactionService deletionTransaction;

    ImageMediaReferenceRemovedListener(ImageMediaDeletionTransactionService deletionTransaction) {
        this.deletionTransaction = deletionTransaction;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void deleteAfterCommit(ImageMediaReferenceRemovedEvent event) {
        Set<String> fileNames = event.imageUrls().stream()
                .map(ImageMediaReferenceGuard::localFileName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (fileNames.isEmpty()) {
            return;
        }
        try {
            deletionTransaction.deleteIfUnreferenced(fileNames);
        } catch (RuntimeException exception) {
            // 커밋된 요청을 실패로 바꾸지 않고, 7일 고아 정리 배치가 다시 회수하게 둔다.
            log.warn("미디어 참조 제거 후 파일 삭제 실패 [fileCount={} type={}]",
                    fileNames.size(), exception.getClass().getSimpleName(), exception);
        }
    }
}
