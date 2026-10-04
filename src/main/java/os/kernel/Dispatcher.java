package os.kernel;

import java.util.ArrayList;
import java.util.List;

/**
 * Records the mechanism of handing the CPU from one process to another.
 * Dispatching after an idle interval is a dispatch, but not a process-to-process
 * context switch.
 */
public final class Dispatcher {
    private final List<Dispatch> history = new ArrayList<>();
    private Integer currentProcessId;
    private int contextSwitches;

    public void dispatch(int processId, int time) {
        if (processId < 0 || time < 0) {
            throw new IllegalArgumentException("Invalid dispatch");
        }
        if (currentProcessId != null && currentProcessId != processId) {
            contextSwitches++;
        }
        if (currentProcessId == null || currentProcessId != processId) {
            history.add(new Dispatch(currentProcessId, processId, time));
        }
        currentProcessId = processId;
    }

    public void idle() {
        currentProcessId = null;
    }

    public int contextSwitches() {
        return contextSwitches;
    }

    public List<Dispatch> history() {
        return List.copyOf(history);
    }

    public record Dispatch(Integer fromProcessId, int toProcessId, int time) {
    }
}
