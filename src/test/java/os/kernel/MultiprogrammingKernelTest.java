package os.kernel;

import org.junit.jupiter.api.Test;
import os.interrupt.InterruptType;
import os.process.ProcessState;
import os.scheduling.RoundRobinPolicy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiprogrammingKernelTest {
    @Test
    void drivesBlockingWakeupPreemptionAndTerminationTransitions() {
        KernelResult result = new MultiprogrammingKernel(
                new RoundRobinPolicy(2)).run(List.of(
                new KernelProcessSpec(1, 0, 0, List.of(1, 1), List.of(2)),
                new KernelProcessSpec(2, 0, 0, List.of(3), List.of())));

        assertEquals(5, result.finishTime());
        assertEquals(ProcessState.TERMINATED, result.finalStates().get(1));
        assertEquals(ProcessState.TERMINATED, result.finalStates().get(2));
        assertEquals(
                List.of(InterruptType.TIMER, InterruptType.IO),
                result.interrupts().stream().map(interrupt -> interrupt.type()).toList());
        assertTrue(result.events().stream().anyMatch(event ->
                event.processId() == 1
                        && event.type() == KernelEvent.Type.IO_REQUEST));
        assertTrue(result.events().stream().anyMatch(event ->
                event.processId() == 1
                        && event.type() == KernelEvent.Type.IO_COMPLETION_INTERRUPT));
        assertEquals(List.of(1, 2, 1, 2),
                result.dispatches().stream()
                        .map(Dispatcher.Dispatch::toProcessId)
                        .toList());
    }
}
