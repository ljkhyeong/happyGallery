package com.personal.happygallery.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.boot.support.EnvironmentPostProcessorsFactory;
import org.springframework.mock.env.MockEnvironment;

class ProductionRuntimeGuardTest {

    @DisplayName("운영 runtime은 prod 단일 프로필과 보안 불변식을 요구한다")
    @Test
    void productionRuntime_requiresProdProfileAndSecurityInvariants() {
        MockEnvironment environment = productionEnvironment();

        assertThatCode(() -> validate(environment))
                .doesNotThrowAnyException();
    }

    @DisplayName("운영 runtime에서 local 프로필이 함께 활성화되면 기동을 거부한다")
    @Test
    void productionRuntime_withLocalProfile_rejectsStartup() {
        MockEnvironment environment = productionEnvironment();
        environment.setActiveProfiles("prod", "local");

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod 프로필만");
    }

    @DisplayName("prod 프로필에서 처리율 제한을 끄면 기동을 거부한다")
    @Test
    void prodProfile_withRateLimitDisabled_rejectsStartup() {
        MockEnvironment environment = productionEnvironment()
                .withProperty("app.rate-limit.enabled", "false");

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.rate-limit.enabled=false");
    }

    @DisplayName("prod 프로필에서 MFA 등록 강제를 끄면 기동을 거부한다")
    @Test
    void prodProfile_withMfaEnrollmentDisabled_rejectsStartup() {
        MockEnvironment environment = productionEnvironment()
                .withProperty("app.admin.require-mfa-enrollment", "false");

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.admin.require-mfa-enrollment=false");
    }

    @DisplayName("개발 runtime은 local 프로필의 개발용 설정을 허용한다")
    @Test
    void developmentRuntime_allowsLocalProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        assertThatCode(() -> validate(environment))
                .doesNotThrowAnyException();
    }

    @DisplayName("운영 runtime guard는 Spring factories 환경 후처리기로 등록된다")
    @Test
    void productionRuntimeGuard_registeredAsEnvironmentPostProcessor() {
        assertThat(EnvironmentPostProcessorsFactory
                .fromSpringFactories(getClass().getClassLoader())
                .getEnvironmentPostProcessors(new DeferredLogs(), new DefaultBootstrapContext()))
                .anyMatch(ProductionRuntimeGuard.class::isInstance);
    }

    @DisplayName("prod와 local 혼합 프로필은 context와 Flyway 초기화 전에 거부한다")
    @Test
    void mixedProductionProfiles_rejectedBeforeApplicationContextInitialization() {
        AtomicBoolean contextInitialized = new AtomicBoolean();
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(Map.of(
                "spring.profiles.active", "prod,local",
                "app.runtime-mode", "production"));
        application.addInitializers(context -> contextInitialized.set(true));

        assertThatThrownBy(application::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod 프로필만");
        assertThat(contextInitialized).isFalse();
    }

    private static MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.runtime-mode", "production")
                .withProperty("app.admin.enable-api-key-auth", "false")
                .withProperty("app.admin.require-mfa-enrollment", "true")
                .withProperty("app.rate-limit.enabled", "true")
                .withProperty("server.servlet.session.cookie.secure", "true");
        environment.setActiveProfiles("prod");
        return environment;
    }

    private static void validate(MockEnvironment environment) {
        new ProductionRuntimeGuard().postProcessEnvironment(environment, null);
    }

    private static final class TestApplication {
    }
}
