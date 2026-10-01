package com.finai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FinanceAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceAiApplication.class, args);
    }
}
