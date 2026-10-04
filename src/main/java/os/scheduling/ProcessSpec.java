package os.scheduling;

/**
 * Immutable workload input. Time and CPU burst are measured in simulator ticks;
 * a lower priority value means a higher scheduling priority.
 */
public record ProcessSpec(int processId, int arrivalTime, int burstTime, int priority) {
    public ProcessSpec {
        if (processId < 0 || arrivalTime < 0 || burstTime <= 0) {
            throw new IllegalArgumentException(
                    "Process id/arrival must be non-negative and burst must be positive");
        }
    }
}
