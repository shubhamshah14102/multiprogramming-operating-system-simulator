package os.scheduling;

public record ProcessMetrics(
        int processId,
        int completionTime,
        int turnaroundTime,
        int waitingTime,
        int responseTime
) {
}
