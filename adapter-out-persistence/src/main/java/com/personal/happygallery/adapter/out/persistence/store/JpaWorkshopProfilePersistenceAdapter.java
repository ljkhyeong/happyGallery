package com.personal.happygallery.adapter.out.persistence.store;

import com.personal.happygallery.application.store.port.out.WorkshopProfileStorePort;
import com.personal.happygallery.domain.store.WorkshopProfile;
import org.springframework.stereotype.Repository;

@Repository
class JpaWorkshopProfilePersistenceAdapter implements WorkshopProfileStorePort {

    private final WorkshopProfileRepository repository;

    JpaWorkshopProfilePersistenceAdapter(WorkshopProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public WorkshopProfile save(WorkshopProfile profile) {
        return repository.save(profile);
    }
}
