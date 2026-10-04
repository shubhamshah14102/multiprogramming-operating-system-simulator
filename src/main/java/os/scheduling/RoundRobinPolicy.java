package os.scheduling;

import java.util.Comparator;
import java.util.List;

public final class RoundRobinPolicy implements SchedulingPolicy {
    private final int quantum;

    public RoundRobinPolicy(int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("Quantum must be positive");
        }
        this.quantum = quantum;
    }

    @Override
    public ProcessSnapshot select(List<ProcessSnapshot> ready, int currentTime) {
        return ready.stream()
                .min(Comparator.comparingLong(ProcessSnapshot::readySequence)
                        .thenComparingInt(ProcessSnapshot::processId))
                .orElseThrow(() -> new IllegalArgumentException("Ready queue is empty"));
    }

    @Override
    public int quantum() {
        return quantum;
    }

    @Override
    public String name() {
        return "ROUND_ROBIN";
    }
}
