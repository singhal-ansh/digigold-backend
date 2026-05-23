package com.fingold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinGoldApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinGoldApplication.class, args);
    }
}
