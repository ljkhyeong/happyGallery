package com.personal.happygallery.config;

import com.personal.happygallery.common.time.Clocks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(Clocks.SEOUL);
    }
}
