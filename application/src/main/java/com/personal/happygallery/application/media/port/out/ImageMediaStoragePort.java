package com.personal.happygallery.application.media.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ImageMediaStoragePort {

    void store(String fileName, byte[] bytes);

    Optional<byte[]> read(String fileName);

    boolean exists(String fileName);

    List<String> findStoredImageNames();

    /** 최초 고아 관찰 시각을 기록하고, 이미 기록된 시각에서 유예 기간이 지났는지 반환한다. */
    boolean markOrphanCandidate(String fileName, Instant observedAt, Duration gracePeriod);

    void clearOrphanMarker(String fileName);

    void delete(String fileName);

    long usedBytes();
}
