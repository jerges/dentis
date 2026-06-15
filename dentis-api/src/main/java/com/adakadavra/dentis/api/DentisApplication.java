package com.adakadavra.dentis.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.adakadavra.dentis")
@EntityScan(basePackages = "com.adakadavra.dentis")
@EnableJpaRepositories(basePackages = "com.adakadavra.dentis")
@EnableAsync
@EnableScheduling
public class DentisApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentisApplication.class, args);
    }
}
