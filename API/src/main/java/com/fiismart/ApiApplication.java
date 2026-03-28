package com.fiismart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.fiismart.student.dashboard",
    "com.fiismart.teacher.dashboard"
})
public class ApiApplication {
    private static final Logger logger = LoggerFactory.getLogger(ApiApplication.class);
    
    public static void main(String[] args) {


        SpringApplication.run(ApiApplication.class, args);

    }
}
