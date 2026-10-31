package com.personal.happygallery.application.store;

import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import com.personal.happygallery.application.store.port.out.WorkshopProfileReaderPort;
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
    private final Clock clock;

    public DefaultWorkshopProfileService(WorkshopProfileReaderPort readerPort, Clock clock) {
        this.readerPort = readerPort;
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
        profile.requireVersion(command.expectedVersion());
        profile.update(
                command.name(), command.phone(), command.postalCode(),
                command.addressLine1(), command.addressLine2(), command.businessHours(),
                command.mapUrl(), command.parkingInfo(), command.businessRegistrationNumber(),
                command.representativeName(), command.email(), command.mailOrderRegistrationNumber(),
                command.introduction(), command.kakaoTalkId(),
                command.naverTalkUrl(), command.naverBlogUrl(),
                command.instagramUrl(), command.smartStoreUrl(),
                LocalDateTime.now(clock));
        // 조회한 영속 엔티티라 커밋 시 변경 감지로 저장되고 version도 그때 올라간다.
        return profile;
    }
}
