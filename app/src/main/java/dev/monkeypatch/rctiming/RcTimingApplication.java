package dev.monkeypatch.rctiming;

import dev.monkeypatch.rctiming.backup.RestoreCommand;
import dev.monkeypatch.rctiming.persistence.DataDirectories;
import dev.monkeypatch.rctiming.simulator.SimulatorMain;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;
import java.util.Arrays;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class RcTimingApplication {
    public static void main(String[] args) throws Exception {
        if (DataDirectories.isMachineScope()) {
            useMachineFolders();
        }
        if (args.length > 0 && args[0].equals("restore")) {
            System.exit(RestoreCommand.run(Arrays.copyOfRange(args, 1, args.length)));
        }
        if (args.length > 0 && args[0].equals("simulate")) {
            SimulatorMain.main(simulatorArguments(Arrays.copyOfRange(args, 1, args.length)));
            return;
        }
        SpringApplication.run(RcTimingApplication.class, args);
    }

    /**
     * The installed service (#23) has no console and no working folder of its own, so it logs to a
     * file in the data folder and reads optional settings (port, bind address and so on) from an
     * {@code application.properties} or {@code application.yml} placed there.
     */
    static void useMachineFolders() {
        String configured = System.getenv("RCTIMING_DATA_DIR");
        Path dataDirectory = configured != null && !configured.isBlank()
                ? Path.of(configured)
                : DataDirectories.defaultDirectory();
        String folderUri = dataDirectory.toAbsolutePath().toUri().toString();
        setIfAbsent("spring.config.additional-location",
                "optional:" + (folderUri.endsWith("/") ? folderUri : folderUri + "/"));
        setIfAbsent("logging.file.name", dataDirectory.resolve("logs").resolve("rctiming.log").toString());
    }

    /**
     * With no arguments, {@code simulate} plays the demo club's eight transponders (#24), so
     * {@code RCTimingControl simulate} next to the demo data gives live laps straight away.
     */
    static String[] simulatorArguments(String[] given) {
        return given.length > 0 ? given : new String[] {
                "--mode=generative", "--port=5100", "--transponders=101,102,103,104,105,106,107,108",
                "--interval-ms=13000", "--jitter-ms=2500"};
    }

    private static void setIfAbsent(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
