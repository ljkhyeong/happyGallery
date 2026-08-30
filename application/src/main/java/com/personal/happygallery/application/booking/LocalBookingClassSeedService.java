package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local")
public class LocalBookingClassSeedService {

    private static final Logger log = LoggerFactory.getLogger(LocalBookingClassSeedService.class);

    private final ClassReaderPort classReaderPort;
    private final ClassStorePort classStorePort;

    public LocalBookingClassSeedService(ClassReaderPort classReaderPort,
                                         ClassStorePort classStorePort) {
        this.classReaderPort = classReaderPort;
        this.classStorePort = classStorePort;
    }

    @Transactional
    public void seedIfEmpty() {
        if (classReaderPort.count() > 0) {
            return;
        }

        List<BookingClass> defaultClasses = List.of(
                localClass(
                        "빈티지 가죽공예 정규", "LEATHER", 120, 50_000L, true,
                        "가죽을 고르고 직접 다듬어 오래 사용할 생활 소품을 완성합니다.",
                        "처음 가죽공예를 접하는 성인과 청소년"),
                localClass(
                        "레진아트 원데이", "RESIN", 90, 42_000L, false,
                        "색과 소재를 조합해 나만의 레진 소품을 만드는 체험 수업입니다.",
                        "개인, 친구, 커플 및 소규모 모임"),
                localClass(
                        "양말목 업사이클링 원데이", "UPCYCLING", 120, 45_000L, false,
                        "버려지는 양말목을 엮어 실용적인 공예 작품으로 되살립니다.",
                        "어린이부터 성인까지 공예를 처음 시작하는 분")
        );

        classStorePort.saveAll(defaultClasses);
        log.info("[LocalSeed] 기본 클래스 {}건을 생성했습니다.", defaultClasses.size());
    }

    private static BookingClass localClass(String name,
                                           String category,
                                           int durationMin,
                                           long price,
                                           boolean passEligible,
                                           String description,
                                           String targetAudience) {
        return new BookingClass(
                name, category, durationMin, price, 30, passEligible,
                description, null, "재료와 도구는 공방에서 준비합니다.", targetAudience);
    }
}
