package os.loader;

import org.junit.jupiter.api.Test;
import os.interrupt.InterruptDispatcher;
import os.machine.MachineMemory;
import os.machine.VirtualCpu;
import os.process.ProcessControlBlock;
import os.process.ProcessState;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobParserLoaderTest {
    private final JobParser parser = new JobParser();
    private final JobLoader loader = new JobLoader();

    @Test
    void parsesAndLoadsJobsInSourceOrder() {
        String source = """
                # deterministic example
                JOB 7 PRIORITY 2
                LOADI R0, 10
                LOADI R1, 3
                SUB R0, R1
                HALT
                END

                JOB 3 PRIORITY 8
                NOP
                HALT
                END
                """;

        List<ProcessControlBlock> processes = loader.load(parser.parse(source));

        assertEquals(List.of(7, 3),
                processes.stream().map(ProcessControlBlock::processId).toList());
        assertEquals(List.of(2, 8),
                processes.stream().map(ProcessControlBlock::priority).toList());
        assertTrue(processes.stream()
                .allMatch(process -> process.state() == ProcessState.NEW));

        VirtualCpu cpu =
                new VirtualCpu(new MachineMemory(32), 100, new InterruptDispatcher());
        cpu.loadProgram(processes.getFirst().program());
        cpu.run(10);
        assertEquals(7, cpu.registers().read(0));
    }

    @Test
    void reportsInputLineForMalformedJobs() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("""
                        JOB 1 PRIORITY 0
                        UNKNOWN R0 4
                        END
                        """));

        assertTrue(exception.getMessage().startsWith("Line 2:"));
    }

    @Test
    void rejectsDuplicateProcessIds() {
        List<JobDefinition> jobs = parser.parse("""
                JOB 5 PRIORITY 1
                HALT
                END
                JOB 5 PRIORITY 9
                HALT
                END
                """);

        assertThrows(IllegalArgumentException.class, () -> loader.load(jobs));
    }
}
