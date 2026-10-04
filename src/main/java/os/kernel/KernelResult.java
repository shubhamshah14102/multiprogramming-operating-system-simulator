package os.kernel;

import os.interrupt.Interrupt;
import os.process.ProcessState;
import os.scheduling.ExecutionSlice;

import java.util.List;
import java.util.Map;

public record KernelResult(
        int finishTime,
        List<ExecutionSlice> timeline,
        List<KernelEvent> events,
        List<Interrupt> interrupts,
        Map<Integer, ProcessState> finalStates,
        List<Dispatcher.Dispatch> dispatches,
        int contextSwitches
) {
    public KernelResult {
        timeline = List.copyOf(timeline);
        events = List.copyOf(events);
        interrupts = List.copyOf(interrupts);
        finalStates = Map.copyOf(finalStates);
        dispatches = List.copyOf(dispatches);
    }
}
