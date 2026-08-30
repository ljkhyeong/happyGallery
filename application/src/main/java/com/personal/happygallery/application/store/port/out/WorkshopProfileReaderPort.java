package com.personal.happygallery.application.store.port.out;

import com.personal.happygallery.domain.store.WorkshopProfile;
import java.util.Optional;

public interface WorkshopProfileReaderPort {
    Optional<WorkshopProfile> findById(Long id);
}
