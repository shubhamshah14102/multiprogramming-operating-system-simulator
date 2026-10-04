package os.kernel;

import java.util.List;

/**
 * Alternating CPU and I/O bursts. There is exactly one fewer I/O burst because
 * a process terminates after its final CPU burst.
 */
public record KernelProcessSpec(
        int processId,
        int arrivalTime,
        int priority,
        List<Integer> cpuBursts,
        List<Integer> ioBursts
) {
    public KernelProcessSpec {
        cpuBursts = List.copyOf(cpuBursts);
        ioBursts = List.copyOf(ioBursts);
        if (processId < 0 || arrivalTime < 0 || cpuBursts.isEmpty()
                || ioBursts.size() != cpuBursts.size() - 1
                || cpuBursts.stream().anyMatch(value -> value <= 0)
                || ioBursts.stream().anyMatch(value -> value <= 0)) {
            throw new IllegalArgumentException("Invalid alternating CPU/I/O burst specification");
        }
    }

    public int totalCpuTime() {
        return cpuBursts.stream().mapToInt(Integer::intValue).sum();
    }
}
