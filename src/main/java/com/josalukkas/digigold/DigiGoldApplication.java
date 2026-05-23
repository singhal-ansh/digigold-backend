package com.josalukkas.digigold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DigiGoldApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigiGoldApplication.class, args);
    }
}
