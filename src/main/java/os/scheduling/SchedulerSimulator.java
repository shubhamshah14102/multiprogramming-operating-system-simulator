package os.scheduling;

import os.kernel.Dispatcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Discrete-event CPU scheduler. Idle time is recorded, not skipped silently. */
public final class SchedulerSimulator {
    private final SchedulingPolicy policy;

    public SchedulerSimulator(SchedulingPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public ScheduleResult run(List<ProcessSpec> workload) {
        Objects.requireNonNull(workload, "workload");
        if (workload.isEmpty()) {
            return new ScheduleResult(policy.name(), List.of(), Map.of(), 0, 0);
        }
        validateUniqueIds(workload);

        List<RuntimeProcess> arrivals = workload.stream()
                .map(RuntimeProcess::new)
                .sorted(Comparator.comparingInt((RuntimeProcess p) -> p.spec.arrivalTime())
                        .thenComparingInt(p -> p.spec.processId()))
                .toList();
        List<RuntimeProcess> ready = new ArrayList<>();
        List<ExecutionSlice> timeline = new ArrayList<>();
        Map<Integer, ProcessMetrics> metrics = new LinkedHashMap<>();
        Dispatcher dispatcher = new Dispatcher();
        long nextSequence = 0;
        int arrivalIndex = 0;
        int time = 0;

        while (metrics.size() < arrivals.size()) {
            while (arrivalIndex < arrivals.size()
                    && arrivals.get(arrivalIndex).spec.arrivalTime() <= time) {
                RuntimeProcess arrived = arrivals.get(arrivalIndex++);
                arrived.readySequence = nextSequence++;
                ready.add(arrived);
            }

            if (ready.isEmpty()) {
                int nextArrival = arrivals.get(arrivalIndex).spec.arrivalTime();
                timeline.add(new ExecutionSlice(
                        ExecutionSlice.IDLE, time, nextArrival - time));
                dispatcher.idle();
                time = nextArrival;
                continue;
            }

            List<ProcessSnapshot> snapshots = ready.stream()
                    .map(RuntimeProcess::snapshot)
                    .toList();
            ProcessSnapshot choice = policy.select(snapshots, time);
            RuntimeProcess running = ready.stream()
                    .filter(process -> process.spec.processId() == choice.processId())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Policy selected a process outside the ready queue"));
            ready.remove(running);
            dispatcher.dispatch(running.spec.processId(), time);
            if (running.firstStart < 0) {
                running.firstStart = time;
            }

            int duration = Math.min(running.remaining, policy.quantum());
            timeline.add(new ExecutionSlice(running.spec.processId(), time, duration));
            running.remaining -= duration;
            time += duration;

            while (arrivalIndex < arrivals.size()
                    && arrivals.get(arrivalIndex).spec.arrivalTime() <= time) {
                RuntimeProcess arrived = arrivals.get(arrivalIndex++);
                arrived.readySequence = nextSequence++;
                ready.add(arrived);
            }

            if (running.remaining == 0) {
                int turnaround = time - running.spec.arrivalTime();
                metrics.put(running.spec.processId(), new ProcessMetrics(
                        running.spec.processId(),
                        time,
                        turnaround,
                        turnaround - running.spec.burstTime(),
                        running.firstStart - running.spec.arrivalTime()));
            } else {
                running.readySequence = nextSequence++;
                ready.add(running);
            }
        }

        return new ScheduleResult(
                policy.name(), timeline, metrics, dispatcher.contextSwitches(), time);
    }

    private static void validateUniqueIds(List<ProcessSpec> workload) {
        Set<Integer> ids = new HashSet<>();
        for (ProcessSpec process : workload) {
            Objects.requireNonNull(process, "process");
            if (!ids.add(process.processId())) {
                throw new IllegalArgumentException(
                        "Duplicate process id: " + process.processId());
            }
        }
    }

    private static final class RuntimeProcess {
        private final ProcessSpec spec;
        private int remaining;
        private int firstStart = -1;
        private long readySequence;

        private RuntimeProcess(ProcessSpec spec) {
            this.spec = spec;
            remaining = spec.burstTime();
        }

        private ProcessSnapshot snapshot() {
            return new ProcessSnapshot(
                    spec.processId(),
                    spec.arrivalTime(),
                    spec.burstTime(),
                    remaining,
                    spec.priority(),
                    readySequence);
        }
    }
}
