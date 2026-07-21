package com.personal.happygallery.application.media;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageMediaRetentionService {

    static final Duration ORPHAN_GRACE_PERIOD = Duration.ofDays(7);
    private final ProductReaderPort productReader;
    private final ClassReaderPort classReader;
    private final ImageMediaStoragePort storagePort;
    private final ImageMediaReferenceGuard referenceGuard;
    private final Clock clock;

    public ImageMediaRetentionService(ProductReaderPort productReader,
                                      ClassReaderPort classReader,
                                      ImageMediaStoragePort storagePort,
                                      ImageMediaReferenceGuard referenceGuard,
                                      Clock clock) {
        this.productReader = productReader;
        this.classReader = classReader;
        this.storagePort = storagePort;
        this.referenceGuard = referenceGuard;
        this.clock = clock;
    }

    @Transactional
    public int deleteUnreferencedImages() {
        referenceGuard.lockForRetention();
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

        if (deletionCandidates.isEmpty()) {
            return 0;
        }

        Set<String> referencedBeforeDelete = referencedFileNames();
        int deleted = 0;
        for (String fileName : deletionCandidates) {
            if (referencedBeforeDelete.contains(fileName)) {
                storagePort.clearOrphanMarker(fileName);
            } else {
                storagePort.delete(fileName);
                deleted++;
            }
        }
        return deleted;
    }

    private Set<String> referencedFileNames() {
        Set<String> referenced = new HashSet<>();
        productReader.findAllProductsByCreatedAtDesc().stream()
                .map(product -> product.getImageUrl())
                .map(ImageMediaReferenceGuard::localFileName)
                .filter(fileName -> fileName != null)
                .forEach(referenced::add);
        classReader.findAll().stream()
                .map(bookingClass -> bookingClass.getImageUrl())
                .map(ImageMediaReferenceGuard::localFileName)
                .filter(fileName -> fileName != null)
                .forEach(referenced::add);
        return referenced;
    }
}
