package os.examples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WalkthroughTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void printsCpuScheduleAndPagingSections() throws Exception {
        Path job = temporaryDirectory.resolve("add.job");
        Files.writeString(job, """
                JOB 1 PRIORITY 1
                LOADI R0 10
                LOADI R1 3
                ADD R0 R1
                STORE R0 20
                HALT
                END
                """);
        StringBuilder output = new StringBuilder();
        Walkthrough.write(output, job);
        String text = output.toString();
        assertTrue(text.contains("CPU trace"));
        assertTrue(text.contains("LOAD_IMMEDIATE"));
        assertTrue(text.contains("page=2 offset=2 frame=1 PA=6"));
        assertTrue(text.contains("RR q=2:"));
        assertTrue(text.contains("FIFO faults="));
        assertTrue(text.contains("LRU faults="));
        assertTrue(text.contains("OPTIMAL faults="));
    }
}
