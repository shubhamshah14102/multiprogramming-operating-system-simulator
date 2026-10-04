package os.benchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import os.benchmark.BenchmarkMetrics.PagingMetrics;
import os.benchmark.BenchmarkMetrics.SchedulingMetrics;
import os.benchmark.BenchmarkWorkloadParser.PagingWorkload;
import os.benchmark.BenchmarkWorkloadParser.SchedulingWorkload;
import os.memory.FifoReplacementPolicy;
import os.memory.PagedVirtualMemory;
import os.scheduling.ExecutionSlice;
import os.scheduling.ProcessMetrics;
import os.scheduling.ScheduleResult;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase3BenchmarkTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesCommittedDefinitionsAtRequiredSizes() throws IOException {
        BenchmarkWorkloadParser parser = new BenchmarkWorkloadParser();

        SchedulingWorkload scheduling = parser.loadDefaultScheduling();
        PagingWorkload paging = parser.loadDefaultPaging();

        assertEquals(20, scheduling.processCount());
        assertEquals(20, scheduling.processes().size());
        assertEquals(1_000, paging.traceLength());
        assertEquals(1_000, paging.references().size());
        assertEquals(4, paging.frameCount());
        assertTrue(paging.references().stream()
                .allMatch(page -> page >= 0 && page < paging.pageCount()));
    }

    @Test
    void parserUsesAllConfigurationFieldsDeterministically() throws IOException {
        String definition = """
                type=scheduling-v1
                seed=7
                process.count=3
                arrival.max=0
                burst.min=2
                burst.max=2
                priority.min=1
                priority.max=1
                """;

        SchedulingWorkload workload = new BenchmarkWorkloadParser()
                .parseScheduling(new StringReader(definition));

        assertEquals(3, workload.processes().size());
        workload.processes().forEach(process -> {
            assertEquals(0, process.arrivalTime());
            assertEquals(2, process.burstTime());
            assertEquals(1, process.priority());
        });
    }

    @Test
    void calculatesSchedulingAndPagingMetrics() {
        ScheduleResult schedule = new ScheduleResult(
                "FCFS",
                List.of(new ExecutionSlice(1, 0, 5),
                        new ExecutionSlice(2, 5, 2)),
                Map.of(
                        1, new ProcessMetrics(1, 5, 5, 0, 0),
                        2, new ProcessMetrics(2, 7, 6, 4, 4)),
                1,
                7);
        SchedulingMetrics scheduling =
                BenchmarkMetrics.scheduling("FCFS", schedule, 7);

        assertEquals(5.5, scheduling.averageTurnaroundTime());
        assertEquals(2.0, scheduling.averageWaitingTime());
        assertEquals(2.0, scheduling.averageResponseTime());
        assertEquals(2.0 / 7.0, scheduling.throughput());
        assertEquals(1.0, scheduling.cpuUtilization());

        PagedVirtualMemory memory =
                new PagedVirtualMemory(3, 1, 2, new FifoReplacementPolicy());
        List.of(0, 1, 0, 2).forEach(memory::read);
        PagingMetrics paging = BenchmarkMetrics.paging("FIFO", 4, 2, memory);

        assertEquals(3, paging.faults());
        assertEquals(1, paging.hits());
        assertEquals(0.75, paging.faultRate());
        assertEquals(List.of(0, 1, 2), paging.faultOrder());
        assertEquals(List.of(0), paging.evictionOrder());
    }

    @Test
    void writesRequiredHumanJsonCsvAndExperimentFields() throws IOException {
        BenchmarkWorkloadParser parser = new BenchmarkWorkloadParser();
        SchedulingWorkload schedulingWorkload = parser.loadDefaultScheduling();
        PagingWorkload pagingWorkload = parser.loadDefaultPaging();
        List<SchedulingMetrics> scheduling =
                BenchmarkDriver.runScheduling(schedulingWorkload);
        List<PagingMetrics> paging = BenchmarkDriver.runPaging(pagingWorkload);

        List<Path> files = new BenchmarkReportWriter().write(
                temporaryDirectory, schedulingWorkload, pagingWorkload,
                scheduling, paging);

        assertEquals(5, files.size());
        String json = Files.readString(
                temporaryDirectory.resolve("benchmark-results.json"));
        assertTrue(json.contains("\"schemaVersion\""));
        assertTrue(json.contains("\"averageResponseTime\""));
        assertTrue(json.contains("\"contextSwitches\""));
        assertTrue(json.contains("\"faults\""));
        assertTrue(json.contains("\"faultOrderPreview\""));
        assertTrue(json.contains("\"evictionCount\""));

        String schedulingCsv = Files.readString(
                temporaryDirectory.resolve("scheduling-results.csv"));
        assertTrue(schedulingCsv.startsWith(
                "policy,process_count,finish_time,context_switches"));
        String pagingCsv = Files.readString(
                temporaryDirectory.resolve("paging-results.csv"));
        assertTrue(pagingCsv.startsWith(
                "policy,reference_count,frame_count,hits,faults"));

        String experiment = Files.readString(
                temporaryDirectory.resolve("EXPERIMENT_RESULTS.md"));
        assertTrue(experiment.contains("RR-vs-FCFS reduction"));
        assertTrue(experiment.contains("NOT SUPPORTED"));
    }
}
