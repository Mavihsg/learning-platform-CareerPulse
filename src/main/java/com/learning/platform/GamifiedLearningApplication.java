package com.learning.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GamifiedLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(GamifiedLearningApplication.class, args);
    }
}
