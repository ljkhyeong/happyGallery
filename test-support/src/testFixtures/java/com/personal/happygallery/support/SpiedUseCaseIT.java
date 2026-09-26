package com.personal.happygallery.support;

import com.personal.happygallery.application.notification.port.out.NotificationOutboxInsertPort;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.external.payment.FakePaymentProvider;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** 실제 Bean 동작을 보존하며 호출·실패를 관찰하는 테스트의 spy 구성을 통일한다. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@UseCaseIT
@MockitoSpyBean(types = {PasswordEncoder.class, NotificationOutboxInsertPort.class, OrderRepository.class})
@MockitoSpyBean(name = "paymentProviderDelegate", types = FakePaymentProvider.class)
public @interface SpiedUseCaseIT {}
