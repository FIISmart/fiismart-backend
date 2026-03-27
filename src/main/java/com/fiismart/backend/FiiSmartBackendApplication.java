package com.fiismart.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.fiismart.backend"})
public class FiiSmartBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FiiSmartBackendApplication.class, args);
    }
}
