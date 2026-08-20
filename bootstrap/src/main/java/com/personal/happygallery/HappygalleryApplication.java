package com.personal.happygallery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableRetry(order = Ordered.LOWEST_PRECEDENCE - 1)
public class HappygalleryApplication {

	public static void main(String[] args) {
		SpringApplication.run(HappygalleryApplication.class, args);
	}

}
