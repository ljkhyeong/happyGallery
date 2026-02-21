package com.personal.happygallery.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 유스케이스 통합 테스트 마커.
 *
 * <p>Spring 컨텍스트를 전체 로드하고 H2(MySQL 모드)로 DB를 대체한다.
 * 핵심 비즈니스 흐름(결제→승인→환불 등)을 end-to-end로 검증하는 테스트에 사용한다.
 *
 * <p>DB 전략: 현재 H2(속도 우선). 운영 DB와의 정합이 중요한 시점에 Testcontainers로 교체한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("usecase")
@SpringBootTest
public @interface UseCaseIT {
}
