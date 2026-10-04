package os.benchmark;

import os.benchmark.BenchmarkMetrics.PagingMetrics;
import os.benchmark.BenchmarkMetrics.SchedulingMetrics;
import os.benchmark.BenchmarkWorkloadParser.PagingWorkload;
import os.benchmark.BenchmarkWorkloadParser.SchedulingWorkload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Writes deterministic human-readable and machine-readable benchmark reports. */
public final class BenchmarkReportWriter {
    public List<Path> write(
            Path outputDirectory,
            SchedulingWorkload schedulingWorkload,
            PagingWorkload pagingWorkload,
            List<SchedulingMetrics> scheduling,
            List<PagingMetrics> paging
    ) throws IOException {
        Files.createDirectories(outputDirectory);
        List<Path> files = new ArrayList<>();
        files.add(writeFile(outputDirectory, "benchmark-summary.txt",
                humanSummary(schedulingWorkload, pagingWorkload, scheduling, paging)));
        files.add(writeFile(outputDirectory, "benchmark-results.json",
                json(schedulingWorkload, pagingWorkload, scheduling, paging)));
        files.add(writeFile(outputDirectory, "scheduling-results.csv",
                schedulingCsv(scheduling)));
        files.add(writeFile(outputDirectory, "paging-results.csv", pagingCsv(paging)));
        files.add(writeFile(outputDirectory, "EXPERIMENT_RESULTS.md",
                experimentDocument(schedulingWorkload, pagingWorkload, scheduling, paging)));
        return List.copyOf(files);
    }

    private static Path writeFile(Path directory, String name, String content)
            throws IOException {
        Path path = directory.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    static String humanSummary(
            SchedulingWorkload schedulingWorkload,
            PagingWorkload pagingWorkload,
            List<SchedulingMetrics> scheduling,
            List<PagingMetrics> paging
    ) {
        StringBuilder output = new StringBuilder();
        output.append("Phase 3 benchmark results\n")
                .append("=========================\n")
                .append(String.format(Locale.ROOT,
                        "Scheduling workload: %d processes, seed %d%n",
                        schedulingWorkload.processCount(), schedulingWorkload.seed()))
                .append("policy, avg response, avg waiting, avg turnaround, switches\n");
        for (SchedulingMetrics metric : scheduling) {
            output.append(String.format(Locale.ROOT,
                    "%s: %.3f, %.3f, %.3f, %d%n",
                    metric.policy(), metric.averageResponseTime(),
                    metric.averageWaitingTime(), metric.averageTurnaroundTime(),
                    metric.contextSwitches()));
        }
        output.append(String.format(Locale.ROOT,
                        "%nPaging workload: %d references, %d frames, seed %d%n",
                        pagingWorkload.traceLength(), pagingWorkload.frameCount(),
                        pagingWorkload.seed()))
                .append("policy, faults, hits, fault rate\n");
        for (PagingMetrics metric : paging) {
            output.append(String.format(Locale.ROOT, "%s: %d, %d, %.3f%%%n",
                    metric.policy(), metric.faults(), metric.hits(),
                    metric.faultRate() * 100));
        }
        output.append(String.format(Locale.ROOT,
                "%nRR-vs-FCFS average response reduction: %.3f%%%n",
                responseReduction(scheduling)));
        return output.toString();
    }

    static String schedulingCsv(List<SchedulingMetrics> metrics) {
        StringBuilder output = new StringBuilder(
                "policy,process_count,finish_time,context_switches,busy_time,"
                        + "average_turnaround_time,average_waiting_time,"
                        + "average_response_time,throughput,cpu_utilization\n");
        for (SchedulingMetrics metric : metrics) {
            output.append(String.format(Locale.ROOT,
                    "%s,%d,%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f%n",
                    metric.policy(), metric.processCount(), metric.finishTime(),
                    metric.contextSwitches(), metric.busyTime(),
                    metric.averageTurnaroundTime(), metric.averageWaitingTime(),
                    metric.averageResponseTime(), metric.throughput(),
                    metric.cpuUtilization()));
        }
        return output.toString();
    }

    static String pagingCsv(List<PagingMetrics> metrics) {
        StringBuilder output = new StringBuilder(
                "policy,reference_count,frame_count,hits,faults,fault_rate,"
                        + "fault_count_check,eviction_count\n");
        for (PagingMetrics metric : metrics) {
            output.append(String.format(
                    Locale.ROOT, "%s,%d,%d,%d,%d,%.6f,%d,%d%n",
                    metric.policy(), metric.referenceCount(), metric.frameCount(),
                    metric.hits(), metric.faults(), metric.faultRate(),
                    metric.faultOrder().size(), metric.evictionOrder().size()));
        }
        return output.toString();
    }

    static String json(
            SchedulingWorkload schedulingWorkload,
            PagingWorkload pagingWorkload,
            List<SchedulingMetrics> scheduling,
            List<PagingMetrics> paging
    ) {
        StringBuilder output = new StringBuilder();
        output.append("{\n  \"schemaVersion\": 1,\n")
                .append("  \"workloads\": {\n")
                .append(String.format(Locale.ROOT,
                        "    \"scheduling\": {\"generator\": \"SplittableRandom\", "
                                + "\"seed\": %d, \"processCount\": %d, "
                                + "\"arrivalMax\": %d, \"burstMin\": %d, "
                                + "\"burstMax\": %d, \"priorityMin\": %d, "
                                + "\"priorityMax\": %d},%n",
                        schedulingWorkload.seed(), schedulingWorkload.processCount(),
                        schedulingWorkload.arrivalMax(), schedulingWorkload.burstMin(),
                        schedulingWorkload.burstMax(), schedulingWorkload.priorityMin(),
                        schedulingWorkload.priorityMax()))
                .append(String.format(Locale.ROOT,
                        "    \"paging\": {\"generator\": \"paging-locality-v1\", "
                                + "\"seed\": %d, \"traceLength\": %d, "
                                + "\"pageCount\": %d, \"frameCount\": %d, "
                                + "\"localityPercent\": %d, \"phaseLength\": %d}%n",
                        pagingWorkload.seed(), pagingWorkload.traceLength(),
                        pagingWorkload.pageCount(), pagingWorkload.frameCount(),
                        pagingWorkload.localityPercent(), pagingWorkload.phaseLength()))
                .append("  },\n  \"scheduling\": [\n");
        for (int index = 0; index < scheduling.size(); index++) {
            SchedulingMetrics metric = scheduling.get(index);
            output.append(String.format(Locale.ROOT,
                    "    {\"policy\": \"%s\", \"processCount\": %d, "
                            + "\"finishTime\": %d, \"contextSwitches\": %d, "
                            + "\"busyTime\": %d, \"averageTurnaroundTime\": %.6f, "
                            + "\"averageWaitingTime\": %.6f, "
                            + "\"averageResponseTime\": %.6f, "
                            + "\"throughput\": %.6f, \"cpuUtilization\": %.6f}%s%n",
                    metric.policy(), metric.processCount(), metric.finishTime(),
                    metric.contextSwitches(), metric.busyTime(),
                    metric.averageTurnaroundTime(), metric.averageWaitingTime(),
                    metric.averageResponseTime(), metric.throughput(),
                    metric.cpuUtilization(),
                    index + 1 == scheduling.size() ? "" : ","));
        }
        output.append("  ],\n  \"paging\": [\n");
        for (int index = 0; index < paging.size(); index++) {
            PagingMetrics metric = paging.get(index);
            output.append(String.format(Locale.ROOT,
                    "    {\"policy\": \"%s\", \"referenceCount\": %d, "
                            + "\"frameCount\": %d, \"hits\": %d, \"faults\": %d, "
                            + "\"faultRate\": %.6f, \"faultOrderPreview\": [%s], "
                            + "\"evictionCount\": %d}%s%n",
                    metric.policy(), metric.referenceCount(), metric.frameCount(),
                    metric.hits(), metric.faults(), metric.faultRate(),
                    preview(metric.faultOrder()),
                    metric.evictionOrder().size(),
                    index + 1 == paging.size() ? "" : ","));
        }
        output.append("  ]\n}\n");
        return output.toString();
    }

    static String experimentDocument(
            SchedulingWorkload schedulingWorkload,
            PagingWorkload pagingWorkload,
            List<SchedulingMetrics> scheduling,
            List<PagingMetrics> paging
    ) {
        SchedulingMetrics fcfs = policy(scheduling, "FCFS");
        SchedulingMetrics rr = policy(scheduling, "RR_Q4");
        double reduction = responseReduction(scheduling);
        boolean supportsSeventyTwo = Math.abs(reduction - 72.0) < 0.0005;
        String pagingOrder = paging.stream()
                .sorted(Comparator.comparingInt(PagingMetrics::faults)
                        .thenComparing(PagingMetrics::policy))
                .map(metric -> metric.policy() + " (" + metric.faults() + ")")
                .collect(Collectors.joining(" < "));

        return String.format(Locale.ROOT, """
                # Phase 3 Experiment Results

                These are measured simulator results, not target values. All scheduling
                policies used the same seeded %d-process workload. All paging policies
                used the same seeded %,d-reference trace and the same %d frames.

                ## Scheduling finding

                FCFS average response time was %.3f ticks and RR quantum 4 average
                response time was %.3f ticks. The measured RR-vs-FCFS reduction was
                **%.3f%%**. Context switches were **%d for FCFS** and **%d for RR**.

                ## Paging finding

                Fault ordering from fewest to most was **%s**. The number in
                parentheses is the actual page-fault count.

                ## Résumé-claim check

                A résumé claim of exactly 72%% lower response time is **%s** by this
                experiment. The defensible claim is the measured %.3f%% reduction above;
                the workload was not tuned to force a target percentage.

                ## Reproduction inputs

                Scheduling seed: `%d`. Paging seed: `%d`. The complete generator
                configuration is committed in `src/main/resources/workloads/` and
                documented in `WORKLOADS.md`.
                """,
                schedulingWorkload.processCount(), pagingWorkload.traceLength(),
                pagingWorkload.frameCount(), fcfs.averageResponseTime(),
                rr.averageResponseTime(), reduction, fcfs.contextSwitches(),
                rr.contextSwitches(), pagingOrder,
                supportsSeventyTwo ? "SUPPORTED" : "NOT SUPPORTED",
                reduction, schedulingWorkload.seed(), pagingWorkload.seed());
    }

    private static String preview(List<Integer> values) {
        int limit = Math.min(8, values.size());
        StringBuilder output = new StringBuilder();
        for (int index = 0; index < limit; index++) {
            if (index > 0) {
                output.append(", ");
            }
            output.append(values.get(index));
        }
        return output.toString();
    }

    private static double responseReduction(List<SchedulingMetrics> metrics) {
        double fcfs = policy(metrics, "FCFS").averageResponseTime();
        double rr = policy(metrics, "RR_Q4").averageResponseTime();
        return fcfs == 0 ? 0 : (fcfs - rr) * 100 / fcfs;
    }

    private static SchedulingMetrics policy(
            List<SchedulingMetrics> metrics, String name) {
        return metrics.stream()
                .filter(metric -> metric.policy().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing scheduling policy: " + name));
    }
}
