package com.personal.happygallery.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 유스케이스 통합 테스트 마커.
 *
 * <p>Spring 컨텍스트를 전체 로드하고 Testcontainers(MySQL 8)로 DB를 구동한다.
 * 핵심 비즈니스 흐름(결제→승인→환불 등)을 end-to-end로 검증하는 테스트에 사용한다.
 *
 * <p>DB 전략: Testcontainers MySQL 8. 운영 DB와 동일한 방언으로 Flyway 마이그레이션까지 검증한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("usecase")
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import(TestcontainersConfig.class)
public @interface UseCaseIT {
}
