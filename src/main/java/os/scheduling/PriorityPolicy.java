package os.scheduling;

import java.util.Comparator;
import java.util.List;

/** Non-preemptive static-priority scheduling; lower numbers run first. */
public final class PriorityPolicy implements SchedulingPolicy {
    @Override
    public ProcessSnapshot select(List<ProcessSnapshot> ready, int currentTime) {
        return ready.stream()
                .min(Comparator.comparingInt(ProcessSnapshot::priority)
                        .thenComparingLong(ProcessSnapshot::readySequence)
                        .thenComparingInt(ProcessSnapshot::processId))
                .orElseThrow(() -> new IllegalArgumentException("Ready queue is empty"));
    }

    @Override
    public String name() {
        return "PRIORITY";
    }
}
