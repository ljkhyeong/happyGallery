package com.personal.happygallery.bootstrap.config;

import com.personal.happygallery.application.admin.LocalAdminSeedService;
import com.personal.happygallery.application.booking.LocalBookingClassSeedService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalSeedConfig {

    @Bean
    public ApplicationRunner localBookingClassSeedRunner(LocalBookingClassSeedService localBookingClassSeedService) {
        return args -> localBookingClassSeedService.seedIfEmpty();
    }

    @Bean
    public ApplicationRunner localAdminSeedRunner(LocalAdminSeedService localAdminSeedService) {
        return args -> localAdminSeedService.seedIfEmpty();
    }
}
