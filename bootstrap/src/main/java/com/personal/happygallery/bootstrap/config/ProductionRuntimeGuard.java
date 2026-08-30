package com.personal.happygallery.bootstrap.config;

import java.util.Arrays;
import java.util.Set;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

public final class ProductionRuntimeGuard implements EnvironmentPostProcessor, Ordered {

    private static final String PRODUCTION_MODE = "production";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application
    ) {
        Set<String> activeProfiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        boolean productionRuntime = PRODUCTION_MODE.equals(environment.getProperty("app.runtime-mode"))
                || activeProfiles.contains("prod");
        if (!productionRuntime) {
            return;
        }

        Assert.state(activeProfiles.equals(Set.of("prod")),
                "운영 runtime은 prod 프로필만 활성화해야 합니다.");
        requireBoolean(environment, "app.admin.enable-api-key-auth", false);
        requireBoolean(environment, "app.admin.require-mfa-enrollment", true);
        requireBoolean(environment, "app.rate-limit.enabled", true);
        requireBoolean(environment, "server.servlet.session.cookie.secure", true);
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    private static void requireBoolean(
            ConfigurableEnvironment environment,
            String propertyName,
            boolean expected
    ) {
        Boolean actual = environment.getProperty(propertyName, Boolean.class);
        Assert.state(actual != null && actual == expected,
                () -> "운영 runtime 설정이 올바르지 않습니다: " + propertyName + "=" + actual);
    }
}
