package ro.fiismart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FiiSmartApplication {

    public static void main(String[] args) {
        SpringApplication.run(FiiSmartApplication.class, args );
    }
}
