package com.personal.happygallery.adapter.out.external.smartstore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SmartStorePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    @DisplayName("스마트스토어 계정 유형을 생략하면 SELF로 설정한다")
    void accountType_defaultsToSelf() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(SmartStoreProperties.class).accountType())
                    .isEqualTo("SELF");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "UNKNOWN"})
    @DisplayName("계정 유형이 비어 있거나 지원하지 않는 값이면 기동을 거절한다")
    void accountType_rejectsInvalidValue(String value) {
        runner.withPropertyValues("app.external.smartstore.account-type=" + value)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("SELLER 연동은 판매자 계정 ID가 있어야 기동한다")
    void seller_requiresAccountId() {
        var sellerRunner = runner.withPropertyValues(
                "app.external.smartstore.enabled=true",
                "app.external.smartstore.client-id=test-client",
                "app.external.smartstore.client-secret=test-secret",
                "app.external.smartstore.account-type=SELLER");
        sellerRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("판매자 계정 ID가 필요합니다");
        });
        sellerRunner.withPropertyValues("app.external.smartstore.account-id=test-seller")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SmartStoreProperties.class)
    static class PropertiesConfig {}
}
