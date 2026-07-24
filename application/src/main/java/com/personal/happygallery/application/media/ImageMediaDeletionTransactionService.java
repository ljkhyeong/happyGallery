package com.personal.happygallery.application.media;

import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort;
import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class ImageMediaDeletionTransactionService {

    private final ImageMediaReferenceReaderPort referenceReader;
    private final ImageMediaStoragePort storagePort;
    private final ImageMediaReferenceGuard referenceGuard;

    ImageMediaDeletionTransactionService(ImageMediaReferenceReaderPort referenceReader,
                                         ImageMediaStoragePort storagePort,
                                         ImageMediaReferenceGuard referenceGuard) {
        this.referenceReader = referenceReader;
        this.storagePort = storagePort;
        this.referenceGuard = referenceGuard;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean deleteIfUnreferenced(String fileName) {
        referenceGuard.lockForRetention();
        boolean referenced = referenceReader.findReferencedImageUrls().stream()
                .map(ImageMediaReferenceGuard::localFileName)
                .anyMatch(fileName::equals);
        if (referenced) {
            storagePort.clearOrphanMarker(fileName);
            return false;
        }
        storagePort.delete(fileName);
        return true;
    }
}
