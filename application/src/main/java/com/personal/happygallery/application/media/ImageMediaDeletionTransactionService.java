package com.personal.happygallery.application.media;

import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort;
import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageMediaDeletionTransactionService {

    private final ImageMediaReferenceReaderPort referenceReader;
    private final ImageMediaStoragePort storagePort;
    private final ImageMediaReferenceGuard referenceGuard;

    public ImageMediaDeletionTransactionService(ImageMediaReferenceReaderPort referenceReader,
                                                 ImageMediaStoragePort storagePort,
                                                 ImageMediaReferenceGuard referenceGuard) {
        this.referenceReader = referenceReader;
        this.storagePort = storagePort;
        this.referenceGuard = referenceGuard;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deleteIfUnreferenced(String fileName) {
        return deleteIfUnreferenced(Set.of(fileName)) == 1;
    }

    /** 같은 커밋에서 제거된 파일들을 한 번의 최신 DB 참조 조회로 재확인한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteIfUnreferenced(Collection<String> fileNames) {
        Set<String> candidates = new LinkedHashSet<>(fileNames);
        if (candidates.isEmpty()) {
            return 0;
        }
        referenceGuard.lockForRetention();
        Set<String> referenced = referenceReader.findReferencedImageUrls().stream()
                .map(ImageMediaReferenceGuard::localFileName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int deleted = 0;
        for (String fileName : candidates) {
            if (referenced.contains(fileName)) {
                storagePort.clearOrphanMarker(fileName);
            } else {
                storagePort.delete(fileName);
                deleted++;
            }
        }
        return deleted;
    }
}
