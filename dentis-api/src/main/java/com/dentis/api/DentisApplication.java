package com.dentis.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.dentis")
@EntityScan(basePackages = "com.dentis.domain.entity")
@EnableJpaRepositories(basePackages = "com.dentis.domain.repository")
@EnableAsync
@EnableScheduling
public class DentisApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentisApplication.class, args);
    }
}
