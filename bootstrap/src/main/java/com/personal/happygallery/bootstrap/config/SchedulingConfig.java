package com.personal.happygallery.bootstrap.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.key-rotation", name = "enabled", havingValue = "false", matchIfMissing = true)
public class SchedulingConfig {}
