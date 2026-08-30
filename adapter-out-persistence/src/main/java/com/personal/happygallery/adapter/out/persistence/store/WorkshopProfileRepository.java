package com.personal.happygallery.adapter.out.persistence.store;

import com.personal.happygallery.application.store.port.out.WorkshopProfileReaderPort;
import com.personal.happygallery.application.store.port.out.WorkshopProfileStorePort;
import com.personal.happygallery.domain.store.WorkshopProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopProfileRepository extends JpaRepository<WorkshopProfile, Long>,
        WorkshopProfileReaderPort,
        WorkshopProfileStorePort {

    @Override
    <S extends WorkshopProfile> S save(S profile);

    @Override
    Optional<WorkshopProfile> findById(Long id);
}
