package com.manacommunity.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "com.manacommunity.api",
        "com.manacommunity.common.security"
})
@EntityScan(basePackages = {
        "com.manacommunity.api",
        "com.manacommunity.common.model",
        "com.manacommunity.common.user.model"
})
@EnableJpaRepositories(basePackages = {
        "com.manacommunity.api",
        "com.manacommunity.common.repository",
        "com.manacommunity.common.user.repository"
})
public class ManaCommunityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManaCommunityServiceApplication.class, args);
    }

}
