package os.scheduling;

import java.util.List;

/**
 * Pluggable ready-queue decision. Policies never mutate process state.
 */
public interface SchedulingPolicy {
    ProcessSnapshot select(List<ProcessSnapshot> ready, int currentTime);

    /**
     * Maximum ticks before reconsidering the ready queue.
     */
    default int quantum() {
        return Integer.MAX_VALUE;
    }

    String name();
}
