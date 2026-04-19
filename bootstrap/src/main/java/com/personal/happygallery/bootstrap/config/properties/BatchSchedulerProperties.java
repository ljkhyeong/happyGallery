package com.personal.happygallery.bootstrap.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.batch.scheduler")
public record BatchSchedulerProperties(
        @Min(1) @DefaultValue("4") int poolSize
) {}
