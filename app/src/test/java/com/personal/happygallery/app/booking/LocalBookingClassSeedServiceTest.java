package com.personal.happygallery.app.booking;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.infra.booking.ClassRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalBookingClassSeedServiceTest {

    @Mock
    private ClassRepository classRepository;

    @DisplayName("클래스가 이미 있으면 로컬 기본 클래스를 추가로 생성하지 않는다")
    @Test
    void seedIfEmpty_whenClassesExist_doesNotCreateDefaults() {
        given(classRepository.count()).willReturn(1L);
        LocalBookingClassSeedService service = new LocalBookingClassSeedService(classRepository);

        service.seedIfEmpty();

        verify(classRepository, never()).saveAll(org.mockito.ArgumentMatchers.<List<BookingClass>>any());
    }

    @DisplayName("클래스가 비어 있으면 로컬 기본 클래스를 생성한다")
    @Test
    @SuppressWarnings("unchecked")
    void seedIfEmpty_whenEmpty_createsDefaults() {
        given(classRepository.count()).willReturn(0L);
        LocalBookingClassSeedService service = new LocalBookingClassSeedService(classRepository);
        ArgumentCaptor<List<BookingClass>> classesCaptor = ArgumentCaptor.forClass(List.class);

        service.seedIfEmpty();

        verify(classRepository).saveAll(classesCaptor.capture());
        List<BookingClass> seededClasses = classesCaptor.getValue();

        assertThat(seededClasses)
                .extracting(BookingClass::getName)
                .containsExactly("향수 클래스", "우드 클래스", "니트 클래스");
        assertThat(seededClasses)
                .extracting(BookingClass::getCategory)
                .containsExactly("PERFUME", "WOOD", "KNIT");
    }
}
