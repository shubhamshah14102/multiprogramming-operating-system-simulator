package os.scheduling;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchedulerSimulatorTest {
    private static final List<ProcessSpec> WORKLOAD = List.of(
            new ProcessSpec(1, 0, 5, 2),
            new ProcessSpec(2, 1, 2, 1),
            new ProcessSpec(3, 1, 1, 0));

    @Test
    void nonPreemptivePoliciesChooseDeterministicallyAndComputeMetrics() {
        ScheduleResult fcfs = new SchedulerSimulator(new FcfsPolicy()).run(WORKLOAD);
        ScheduleResult sjf = new SchedulerSimulator(new SjfPolicy()).run(WORKLOAD);
        ScheduleResult priority =
                new SchedulerSimulator(new PriorityPolicy()).run(WORKLOAD);

        assertEquals(List.of(1, 2, 3), fcfs.dispatchOrder());
        assertEquals(List.of(1, 3, 2), sjf.dispatchOrder());
        assertEquals(List.of(1, 3, 2), priority.dispatchOrder());
        assertEquals(new ProcessMetrics(2, 7, 6, 4, 4), fcfs.metrics().get(2));
        assertEquals(8, fcfs.finishTime());
    }

    @Test
    void roundRobinRequeuesAfterArrivalsAtTheQuantumBoundary() {
        ScheduleResult result =
                new SchedulerSimulator(new RoundRobinPolicy(2)).run(WORKLOAD);

        assertEquals(List.of(1, 2, 3, 1, 1), result.dispatchOrder());
        assertEquals(1, result.metrics().get(2).responseTime());
        assertEquals(8, result.finishTime());
    }

    @Test
    void recordsIdleGapsInsteadOfInventingWork() {
        ScheduleResult result = new SchedulerSimulator(new FcfsPolicy()).run(List.of(
                new ProcessSpec(7, 3, 1, 0),
                new ProcessSpec(8, 6, 1, 0)));

        assertEquals(
                List.of(
                        new ExecutionSlice(ExecutionSlice.IDLE, 0, 3),
                        new ExecutionSlice(7, 3, 1),
                        new ExecutionSlice(ExecutionSlice.IDLE, 4, 2),
                        new ExecutionSlice(8, 6, 1)),
                result.timeline());
    }
}
