package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalBookingClassSeedServiceTest {

    @Mock
    private ClassReaderPort classReaderPort;

    @Mock
    private ClassStorePort classStorePort;

    @DisplayName("클래스가 이미 있으면 로컬 기본 클래스를 추가로 생성하지 않는다")
    @Test
    void seedIfEmpty_whenClassesExist_doesNotCreateDefaults() {
        given(classReaderPort.count()).willReturn(1L);
        LocalBookingClassSeedService service = new LocalBookingClassSeedService(classReaderPort, classStorePort);

        service.seedIfEmpty();

        verify(classStorePort, never()).saveAll(any());
    }

    @DisplayName("클래스가 비어 있으면 로컬 기본 클래스를 생성한다")
    @Test
    @SuppressWarnings("unchecked")
    void seedIfEmpty_whenEmpty_createsDefaults() {
        given(classReaderPort.count()).willReturn(0L);
        LocalBookingClassSeedService service = new LocalBookingClassSeedService(classReaderPort, classStorePort);
        ArgumentCaptor<List<BookingClass>> classesCaptor = ArgumentCaptor.forClass(List.class);

        service.seedIfEmpty();

        verify(classStorePort).saveAll(classesCaptor.capture());
        List<BookingClass> seededClasses = classesCaptor.getValue();

        assertSoftly(softly -> {
            softly.assertThat(seededClasses)
                    .extracting(BookingClass::getName)
                    .containsExactly("향수 클래스", "우드 클래스", "니트 클래스");
            softly.assertThat(seededClasses)
                    .extracting(BookingClass::getCategory)
                    .containsExactly("PERFUME", "WOOD", "KNIT");
        });
    }
}
