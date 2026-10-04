package os.scheduling;

import java.util.Comparator;
import java.util.List;

/** Non-preemptive shortest-job-first. */
public final class SjfPolicy implements SchedulingPolicy {
    @Override
    public ProcessSnapshot select(List<ProcessSnapshot> ready, int currentTime) {
        return ready.stream()
                .min(Comparator.comparingInt(ProcessSnapshot::remainingTime)
                        .thenComparingLong(ProcessSnapshot::readySequence)
                        .thenComparingInt(ProcessSnapshot::processId))
                .orElseThrow(() -> new IllegalArgumentException("Ready queue is empty"));
    }

    @Override
    public String name() {
        return "SJF";
    }
}
