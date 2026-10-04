package os.benchmark;

import os.memory.PageFaultEvent;
import os.memory.PagedVirtualMemory;
import os.scheduling.ProcessMetrics;
import os.scheduling.ScheduleResult;

import java.util.List;

/** Pure calculations over simulator observations used by reports and tests. */
public final class BenchmarkMetrics {
    private BenchmarkMetrics() {
    }

    public static SchedulingMetrics scheduling(
            String displayPolicy, ScheduleResult result, int totalBurstTime) {
        int processCount = result.metrics().size();
        if (processCount == 0) {
            return new SchedulingMetrics(
                    displayPolicy, 0, result.finishTime(), result.contextSwitches(),
                    0, 0, 0, 0, 0, 0);
        }
        double turnaround = result.metrics().values().stream()
                .mapToInt(ProcessMetrics::turnaroundTime).average().orElseThrow();
        double waiting = result.metrics().values().stream()
                .mapToInt(ProcessMetrics::waitingTime).average().orElseThrow();
        double response = result.metrics().values().stream()
                .mapToInt(ProcessMetrics::responseTime).average().orElseThrow();
        double throughput = result.finishTime() == 0
                ? 0 : (double) processCount / result.finishTime();
        double utilization = result.finishTime() == 0
                ? 0 : (double) totalBurstTime / result.finishTime();
        return new SchedulingMetrics(
                displayPolicy, processCount, result.finishTime(),
                result.contextSwitches(), totalBurstTime, turnaround, waiting,
                response, throughput, utilization);
    }

    public static PagingMetrics paging(
            String policy, int referenceCount, int frameCount,
            PagedVirtualMemory memory) {
        int faults = Math.toIntExact(memory.pageFaultCount());
        int hits = referenceCount - faults;
        List<Integer> faultOrder = memory.pageFaultHistory().stream()
                .map(PageFaultEvent::requestedPage)
                .toList();
        List<Integer> evictionOrder = memory.pageFaultHistory().stream()
                .map(PageFaultEvent::evictedPage)
                .filter(page -> page != null)
                .toList();
        return new PagingMetrics(
                policy, referenceCount, frameCount, hits, faults,
                referenceCount == 0 ? 0 : (double) faults / referenceCount,
                faultOrder, evictionOrder);
    }

    public record SchedulingMetrics(
            String policy,
            int processCount,
            int finishTime,
            int contextSwitches,
            int busyTime,
            double averageTurnaroundTime,
            double averageWaitingTime,
            double averageResponseTime,
            double throughput,
            double cpuUtilization
    ) {
    }

    public record PagingMetrics(
            String policy,
            int referenceCount,
            int frameCount,
            int hits,
            int faults,
            double faultRate,
            List<Integer> faultOrder,
            List<Integer> evictionOrder
    ) {
        public PagingMetrics {
            faultOrder = List.copyOf(faultOrder);
            evictionOrder = List.copyOf(evictionOrder);
        }
    }
}
