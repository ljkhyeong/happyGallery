package com.personal.happygallery.application.media;

import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort;
import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ImageMediaRetentionService {

    static final Duration ORPHAN_GRACE_PERIOD = Duration.ofDays(7);
    private final ImageMediaReferenceReaderPort referenceReader;
    private final ImageMediaStoragePort storagePort;
    private final ImageMediaDeletionTransactionService deletionTransaction;
    private final Clock clock;

    public ImageMediaRetentionService(ImageMediaReferenceReaderPort referenceReader,
                                      ImageMediaStoragePort storagePort,
                                      ImageMediaDeletionTransactionService deletionTransaction,
                                      Clock clock) {
        this.referenceReader = referenceReader;
        this.storagePort = storagePort;
        this.deletionTransaction = deletionTransaction;
        this.clock = clock;
    }

    public int deleteUnreferencedImages() {
        Set<String> referencedAtStart = referencedFileNames();
        Instant observedAt = clock.instant();
        List<String> deletionCandidates = new ArrayList<>();
        for (String fileName : storagePort.findStoredImageNames()) {
            if (referencedAtStart.contains(fileName)) {
                storagePort.clearOrphanMarker(fileName);
            } else if (storagePort.markOrphanCandidate(fileName, observedAt, ORPHAN_GRACE_PERIOD)) {
                deletionCandidates.add(fileName);
            }
        }

        int deleted = 0;
        for (String fileName : deletionCandidates) {
            if (deletionTransaction.deleteIfUnreferenced(fileName)) {
                deleted++;
            }
        }
        return deleted;
    }

    private Set<String> referencedFileNames() {
        Set<String> referenced = new HashSet<>();
        referenceReader.findReferencedImageUrls().stream()
                .map(ImageMediaReferenceGuard::localFileName)
                .filter(fileName -> fileName != null)
                .forEach(referenced::add);
        return referenced;
    }
}
