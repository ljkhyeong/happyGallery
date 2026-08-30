package com.personal.happygallery.adapter.out.external.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ExternalCircuitBreakerMetricsConfig {

    @Bean
    CircuitBreakerRegistry externalCircuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    MeterBinder externalCircuitBreakerMetrics(CircuitBreakerRegistry circuitBreakerRegistry) {
        return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry);
    }
}
