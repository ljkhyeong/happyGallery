package com.personal.happygallery.application.store.port.out;

import com.personal.happygallery.domain.store.WorkshopProfile;

public interface WorkshopProfileStorePort {

    <S extends WorkshopProfile> S save(S profile);
}
