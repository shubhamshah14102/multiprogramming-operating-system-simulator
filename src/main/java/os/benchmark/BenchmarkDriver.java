package os.benchmark;

import os.benchmark.BenchmarkMetrics.PagingMetrics;
import os.benchmark.BenchmarkMetrics.SchedulingMetrics;
import os.benchmark.BenchmarkWorkloadParser.PagingWorkload;
import os.benchmark.BenchmarkWorkloadParser.SchedulingWorkload;
import os.memory.FifoReplacementPolicy;
import os.memory.LruReplacementPolicy;
import os.memory.OptimalReplacementPolicy;
import os.memory.PageReplacementPolicy;
import os.memory.PagedVirtualMemory;
import os.scheduling.FcfsPolicy;
import os.scheduling.PriorityPolicy;
import os.scheduling.RoundRobinPolicy;
import os.scheduling.ScheduleResult;
import os.scheduling.SchedulerSimulator;
import os.scheduling.SchedulingPolicy;
import os.scheduling.SjfPolicy;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Runs the committed scheduling and paging experiments. */
public final class BenchmarkDriver {
    private BenchmarkDriver() {
    }

    public static void main(String[] args) throws IOException {
        Arguments arguments = Arguments.parse(args);
        BenchmarkWorkloadParser parser = new BenchmarkWorkloadParser();
        SchedulingWorkload schedulingWorkload =
                arguments.schedulingDefinition() == null
                        ? parser.loadDefaultScheduling()
                        : parseScheduling(parser, arguments.schedulingDefinition());
        PagingWorkload pagingWorkload =
                arguments.pagingDefinition() == null
                        ? parser.loadDefaultPaging()
                        : parsePaging(parser, arguments.pagingDefinition());

        List<SchedulingMetrics> scheduling = runScheduling(schedulingWorkload);
        List<PagingMetrics> paging = runPaging(pagingWorkload);
        List<Path> files = new BenchmarkReportWriter().write(
                arguments.outputDirectory(), schedulingWorkload, pagingWorkload,
                scheduling, paging);

        System.out.print(BenchmarkReportWriter.humanSummary(
                schedulingWorkload, pagingWorkload, scheduling, paging));
        System.out.println("Wrote:");
        files.forEach(path -> System.out.println("  " + path.toAbsolutePath()));
    }

    static List<SchedulingMetrics> runScheduling(SchedulingWorkload workload) {
        int busyTime = workload.processes().stream()
                .mapToInt(process -> process.burstTime()).sum();
        List<SchedulingRun> runs = List.of(
                new SchedulingRun("FCFS", FcfsPolicy::new),
                new SchedulingRun("SJF_NON_PREEMPTIVE", SjfPolicy::new),
                new SchedulingRun("RR_Q4", () -> new RoundRobinPolicy(4)),
                new SchedulingRun(
                        "PRIORITY_NON_PREEMPTIVE", PriorityPolicy::new));
        List<SchedulingMetrics> metrics = new ArrayList<>(runs.size());
        for (SchedulingRun run : runs) {
            ScheduleResult result = new SchedulerSimulator(run.policy().get())
                    .run(workload.processes());
            metrics.add(BenchmarkMetrics.scheduling(
                    run.displayName(), result, busyTime));
        }
        return List.copyOf(metrics);
    }

    static List<PagingMetrics> runPaging(PagingWorkload workload) {
        List<PageReplacementPolicy> policies = List.of(
                new FifoReplacementPolicy(),
                new LruReplacementPolicy(),
                new OptimalReplacementPolicy(workload.references()));
        List<PagingMetrics> metrics = new ArrayList<>(policies.size());
        for (PageReplacementPolicy policy : policies) {
            PagedVirtualMemory memory = new PagedVirtualMemory(
                    workload.pageCount(), 1, workload.frameCount(), policy);
            workload.references().forEach(memory::read);
            metrics.add(BenchmarkMetrics.paging(
                    policy.name(), workload.traceLength(),
                    workload.frameCount(), memory));
        }
        return List.copyOf(metrics);
    }

    private static SchedulingWorkload parseScheduling(
            BenchmarkWorkloadParser parser, Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parser.parseScheduling(reader);
        }
    }

    private static PagingWorkload parsePaging(
            BenchmarkWorkloadParser parser, Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parser.parsePaging(reader);
        }
    }

    private record SchedulingRun(
            String displayName, Supplier<SchedulingPolicy> policy) {
    }

    private record Arguments(
            Path outputDirectory,
            Path schedulingDefinition,
            Path pagingDefinition
    ) {
        private static Arguments parse(String[] args) {
            Path output = Path.of("results");
            Path scheduling = null;
            Path paging = null;
            for (int index = 0; index < args.length; index += 2) {
                if (index + 1 >= args.length) {
                    throw usage("Missing value for " + args[index]);
                }
                Path value = Path.of(args[index + 1]);
                switch (args[index]) {
                    case "--output" -> output = value;
                    case "--scheduling" -> scheduling = value;
                    case "--paging" -> paging = value;
                    default -> throw usage("Unknown option: " + args[index]);
                }
            }
            return new Arguments(output, scheduling, paging);
        }

        private static IllegalArgumentException usage(String message) {
            return new IllegalArgumentException(message
                    + ". Usage: BenchmarkDriver [--output DIR] "
                    + "[--scheduling FILE] [--paging FILE]");
        }
    }
}
