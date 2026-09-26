package com.personal.happygallery.support;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import com.personal.happygallery.application.order.port.out.KoreaPostTrackingLookup;
import com.personal.happygallery.application.order.port.out.ShipmentTrackingProvider;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 외부 연동의 응답을 직접 제어하는 테스트끼리 같은 context를 사용한다. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@UseCaseIT
@MockitoBean(types = {PhoneVerificationSender.class, KoreaPostTrackingLookup.class,
        ShipmentTrackingProvider.class, SmartStoreOrderProvider.class, SmartStoreInventoryProvider.class})
public @interface ExternalIntegrationUseCaseIT {}
