package com.personal.happygallery.application.media;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.booking.DefaultClassManagementService;
import com.personal.happygallery.application.booking.port.in.ClassManagementUseCase.CreateClassCommand;
import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.coupon.port.out.CouponDefinitionReaderPort;
import com.personal.happygallery.application.event.DefaultEventService;
import com.personal.happygallery.application.event.port.in.EventAdminUseCase.CreateCommand;
import com.personal.happygallery.application.event.port.out.EventReaderPort;
import com.personal.happygallery.application.event.port.out.EventStorePort;
import com.personal.happygallery.application.product.DefaultProductAdminService;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.product.ProductOptionConfigurationService;
import com.personal.happygallery.application.product.ProductVariantStockService;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SaveProductCommand;
import com.personal.happygallery.application.product.port.out.InventoryAdjustmentHistoryPort;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.event.Event;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CatalogImageAssignmentNormalizationTest {

    private static final String RAW_IMAGE_URL =
            "  /api/v1/media/images/11111111-1111-4111-8111-111111111111.jpg?version=1#preview  ";
    private static final String NORMALIZED_IMAGE_URL =
            "/api/v1/media/images/11111111-1111-4111-8111-111111111111.jpg";

    @Test
    @DisplayName("상품은 도메인이 정규화한 최종 이미지 URL로 파일 존재를 검증한다")
    void productUsesNormalizedImageUrlForAssignmentGuard() {
        ProductStorePort productStore = mock(ProductStorePort.class);
        InventoryService inventoryService = mock(InventoryService.class);
        ImageMediaReferenceGuard guard = mock(ImageMediaReferenceGuard.class);
        when(productStore.save(any(Product.class))).thenAnswer(returnsFirstArg());
        when(inventoryService.create(any(Product.class), anyInt()))
                .thenReturn(mock(Inventory.class));
        DefaultProductAdminService service = new DefaultProductAdminService(
                productStore,
                mock(ProductReaderPort.class),
                mock(InventoryReaderPort.class),
                mock(InventoryAdjustmentHistoryPort.class),
                inventoryService,
                mock(ProductVariantStockService.class),
                mock(ProductOptionConfigurationService.class),
                guard,
                Clock.systemUTC());

        service.register(new SaveProductCommand(
                "원목 트레이", ProductType.READY_STOCK, "WOOD", 30_000L, 1,
                null, RAW_IMAGE_URL, null, null, null, List.of(), List.of()));

        verify(guard).validateAssignment(NORMALIZED_IMAGE_URL);
    }

    @Test
    @DisplayName("클래스는 도메인이 정규화한 최종 이미지 URL로 파일 존재를 검증한다")
    void bookingClassUsesNormalizedImageUrlForAssignmentGuard() {
        ClassStorePort classStore = mock(ClassStorePort.class);
        ImageMediaReferenceGuard guard = mock(ImageMediaReferenceGuard.class);
        when(classStore.save(any(BookingClass.class))).thenAnswer(returnsFirstArg());
        DefaultClassManagementService service = new DefaultClassManagementService(
                classStore,
                mock(ClassReaderPort.class),
                guard);

        service.createClass(new CreateClassCommand(
                "향수 클래스",
                "PERFUME",
                120,
                50_000L,
                30,
                8,
                true,
                null,
                RAW_IMAGE_URL,
                null,
                null));

        verify(guard).validateAssignment(NORMALIZED_IMAGE_URL);
    }

    @Test
    @DisplayName("이벤트는 도메인이 정규화한 최종 이미지 URL로 파일 존재를 검증한다")
    void eventUsesNormalizedImageUrlForAssignmentGuard() {
        EventStorePort eventStore = mock(EventStorePort.class);
        ImageMediaReferenceGuard guard = mock(ImageMediaReferenceGuard.class);
        when(eventStore.save(any(Event.class))).thenAnswer(returnsFirstArg());
        DefaultEventService service = new DefaultEventService(
                mock(EventReaderPort.class),
                eventStore,
                mock(ProductReaderPort.class),
                mock(CouponDefinitionReaderPort.class),
                guard,
                Clock.systemUTC());
        LocalDateTime now = LocalDateTime.of(2026, 8, 14, 21, 0);

        service.create(new CreateCommand(
                "여름 공방전",
                "여름 행사",
                "행사 내용",
                RAW_IMAGE_URL,
                now.minusDays(1),
                now.plusDays(1),
                true,
                true,
                null,
                Set.of()));

        verify(guard).validateAssignment(NORMALIZED_IMAGE_URL);
    }
}
