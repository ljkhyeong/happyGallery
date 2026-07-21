package com.personal.happygallery.application.store;

import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import com.personal.happygallery.application.store.port.out.WorkshopProfileReaderPort;
import com.personal.happygallery.application.store.port.out.WorkshopProfileStorePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.store.WorkshopProfile;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultWorkshopProfileService implements WorkshopProfileUseCase {

    private final WorkshopProfileReaderPort readerPort;
    private final WorkshopProfileStorePort storePort;
    private final Clock clock;

    public DefaultWorkshopProfileService(WorkshopProfileReaderPort readerPort,
                                         WorkshopProfileStorePort storePort,
                                         Clock clock) {
        this.readerPort = readerPort;
        this.storePort = storePort;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkshopProfile get() {
        return readerPort.findById(WorkshopProfile.SINGLETON_ID)
                .orElseThrow(NotFoundException.supplier("공방 정보"));
    }

    @Override
    public WorkshopProfile update(UpdateCommand command) {
        WorkshopProfile profile = get();
        profile.update(
                command.name(), command.phone(), command.postalCode(),
                command.addressLine1(), command.addressLine2(), command.businessHours(),
                command.mapUrl(), command.parkingInfo(), command.businessRegistrationNumber(),
                command.representativeName(), command.email(), command.mailOrderRegistrationNumber(),
                command.introduction(), command.kakaoTalkId(), command.naverTalkEnabled(),
                LocalDateTime.now(clock));
        return storePort.save(profile);
    }
}
