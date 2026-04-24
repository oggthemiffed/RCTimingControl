package dev.monkeypatch.rctiming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RcTimingApplication {
    public static void main(String[] args) {
        SpringApplication.run(RcTimingApplication.class, args);
    }
}
