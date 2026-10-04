package dev.monkeypatch.rctiming;

import dev.monkeypatch.rctiming.backup.RestoreCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class RcTimingApplication {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("restore")) {
            System.exit(RestoreCommand.run(Arrays.copyOfRange(args, 1, args.length)));
        }
        SpringApplication.run(RcTimingApplication.class, args);
    }
}
