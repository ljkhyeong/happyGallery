package com.personal.happygallery.application.search.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdminPassQueryPort {

    List<AdminPassQueryResult> search(String keyword, LocalDateTime now, int offset, int size);

    long count(String keyword);

    Optional<AdminPassQueryResult> findById(Long passId, LocalDateTime now);
}
