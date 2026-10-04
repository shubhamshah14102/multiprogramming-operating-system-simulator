package os.scheduling;

import java.util.Comparator;
import java.util.List;

public final class FcfsPolicy implements SchedulingPolicy {
    private static final Comparator<ProcessSnapshot> ORDER =
            Comparator.comparingLong(ProcessSnapshot::readySequence)
                    .thenComparingInt(ProcessSnapshot::processId);

    @Override
    public ProcessSnapshot select(List<ProcessSnapshot> ready, int currentTime) {
        return ready.stream().min(ORDER)
                .orElseThrow(() -> new IllegalArgumentException("Ready queue is empty"));
    }

    @Override
    public String name() {
        return "FCFS";
    }
}
