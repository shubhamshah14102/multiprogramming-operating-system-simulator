package os.scheduling;

/**
 * Read-only state exposed to a scheduling policy. readySequence is assigned by
 * the simulator and provides deterministic queue ordering.
 */
public record ProcessSnapshot(
        int processId,
        int arrivalTime,
        int burstTime,
        int remainingTime,
        int priority,
        long readySequence
) {
}
