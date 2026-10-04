package dev.monkeypatch.rctiming.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pulling the plug on the venue laptop mid-session must not lose a lap the app had committed
 * (#26). The app runs in a child JVM writing practice laps; the test kills it without warning,
 * starts the app again on the same database and checks every committed lap is still there.
 */
class CrashRecoveryIT {

    private static final int LAPS_BEFORE_KILL = 200;

    @TempDir
    Path dataDirectory;

    @Test
    void killedMidSessionKeepsEveryCommittedLap() throws Exception {
        Process writer = startChild("write");
        long lastCommitted = 0;
        try (BufferedReader out = reader(writer)) {
            String line;
            while (lastCommitted < LAPS_BEFORE_KILL && (line = out.readLine()) != null) {
                if (line.startsWith(CrashRecoveryChild.COMMITTED)) {
                    lastCommitted = Long.parseLong(line.substring(CrashRecoveryChild.COMMITTED.length()).trim());
                }
            }
            writer.destroyForcibly();
            assertThat(writer.waitFor(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            writer.destroyForcibly();
        }
        assertThat(lastCommitted).as("the child committed laps before it was killed").isEqualTo(LAPS_BEFORE_KILL);

        Process counter = startChild("count");
        List<String> output = new ArrayList<>();
        long survived = -1;
        try (BufferedReader out = reader(counter)) {
            String line;
            while ((line = out.readLine()) != null) {
                output.add(line);
                if (line.startsWith(CrashRecoveryChild.SURVIVED)) {
                    survived = Long.parseLong(line.substring(CrashRecoveryChild.SURVIVED.length()).trim());
                }
            }
            assertThat(counter.waitFor(2, TimeUnit.MINUTES)).isTrue();
        } finally {
            counter.destroyForcibly();
        }
        assertThat(counter.exitValue()).as("restart output:%n%s", String.join("\n", output)).isZero();
        // The kill can land after a commit but before its line was printed, so more may survive
        assertThat(survived).isGreaterThanOrEqualTo(lastCommitted);
    }

    private Process startChild(String mode) throws IOException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        return new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                CrashRecoveryChild.class.getName(), dataDirectory.toString(), mode)
                .redirectErrorStream(true)
                .start();
    }

    private static BufferedReader reader(Process process) {
        return new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    }
}
