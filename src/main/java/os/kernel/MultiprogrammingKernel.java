package os.kernel;

import os.interrupt.Interrupt;
import os.machine.Instruction;
import os.machine.Opcode;
import os.machine.Program;
import os.process.ProcessControlBlock;
import os.process.ProcessState;
import os.scheduling.ExecutionSlice;
import os.scheduling.ProcessSnapshot;
import os.scheduling.SchedulingPolicy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Tick-driven kernel: admit, dispatch, preempt on the quantum, block on I/O,
 * wake on I/O completion, terminate.
 */
public final class MultiprogrammingKernel {
    private final SchedulingPolicy policy;

    public MultiprogrammingKernel(SchedulingPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public KernelResult run(List<KernelProcessSpec> workload) {
        Objects.requireNonNull(workload, "workload");
        validate(workload);
        if (workload.isEmpty()) {
            return new KernelResult(
                    0, List.of(), List.of(), List.of(), Map.of(), List.of(), 0);
        }

        List<RuntimeProcess> processes = workload.stream()
                .map(RuntimeProcess::new)
                .sorted(Comparator.comparingInt((RuntimeProcess p) -> p.spec.arrivalTime())
                        .thenComparingInt(p -> p.spec.processId()))
                .toList();
        List<RuntimeProcess> ready = new ArrayList<>();
        List<ExecutionSlice> timeline = new ArrayList<>();
        List<KernelEvent> events = new ArrayList<>();
        List<Interrupt> interrupts = new ArrayList<>();
        Dispatcher dispatcher = new Dispatcher();
        RuntimeProcess running = null;
        RuntimeProcess requeueAfterAdmissions = null;
        long readySequence = 0;
        int time = 0;
        int quantumUsed = 0;
        int terminated = 0;

        while (terminated < processes.size()) {
            for (RuntimeProcess process : processes) {
                if (!process.admitted && process.spec.arrivalTime() == time) {
                    process.admitted = true;
                    process.pcb.transitionTo(ProcessState.READY);
                    process.readySequence = readySequence++;
                    ready.add(process);
                    events.add(new KernelEvent(
                            time, process.spec.processId(),
                            KernelEvent.Type.ARRIVAL, "NEW -> READY"));
                }
                if (process.pcb.state() == ProcessState.BLOCKED
                        && process.unblockAt == time) {
                    process.pcb.transitionTo(ProcessState.READY);
                    process.readySequence = readySequence++;
                    ready.add(process);
                    Interrupt interrupt = Interrupt.io(
                            process.spec.processId(), "scripted-device");
                    interrupts.add(interrupt);
                    events.add(new KernelEvent(
                            time, process.spec.processId(),
                            KernelEvent.Type.IO_COMPLETION_INTERRUPT,
                            "BLOCKED -> READY"));
                }
            }
            if (requeueAfterAdmissions != null) {
                requeueAfterAdmissions.readySequence = readySequence++;
                ready.add(requeueAfterAdmissions);
                requeueAfterAdmissions = null;
            }

            if (running == null && !ready.isEmpty()) {
                ProcessSnapshot choice = policy.select(
                        ready.stream().map(RuntimeProcess::snapshot).toList(), time);
                running = ready.stream()
                        .filter(process -> process.spec.processId() == choice.processId())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Policy selected a process outside the ready queue"));
                ready.remove(running);
                running.pcb.transitionTo(ProcessState.RUNNING);
                dispatcher.dispatch(running.spec.processId(), time);
                quantumUsed = 0;
                events.add(new KernelEvent(
                        time, running.spec.processId(),
                        KernelEvent.Type.DISPATCH, "READY -> RUNNING"));
            }

            if (running == null) {
                appendTick(timeline, ExecutionSlice.IDLE, time);
                dispatcher.idle();
                events.add(new KernelEvent(
                        time, ExecutionSlice.IDLE, KernelEvent.Type.IDLE, "CPU idle"));
                time++;
                continue;
            }

            appendTick(timeline, running.spec.processId(), time);
            running.remainingInBurst--;
            quantumUsed++;
            time++;

            if (running.remainingInBurst == 0) {
                if (running.burstIndex == running.spec.cpuBursts().size() - 1) {
                    running.pcb.transitionTo(ProcessState.TERMINATED);
                    events.add(new KernelEvent(
                            time, running.spec.processId(),
                            KernelEvent.Type.TERMINATION, "RUNNING -> TERMINATED"));
                    terminated++;
                } else {
                    running.pcb.transitionTo(ProcessState.BLOCKED);
                    int ioDuration = running.spec.ioBursts().get(running.burstIndex);
                    running.unblockAt = time + ioDuration;
                    running.burstIndex++;
                    running.remainingInBurst =
                            running.spec.cpuBursts().get(running.burstIndex);
                    events.add(new KernelEvent(
                            time, running.spec.processId(),
                            KernelEvent.Type.IO_REQUEST,
                            "RUNNING -> BLOCKED for " + ioDuration + " ticks"));
                }
                running = null;
            } else if (quantumUsed >= policy.quantum()) {
                running.pcb.transitionTo(ProcessState.READY);
                Interrupt timer = Interrupt.timer(quantumUsed);
                interrupts.add(timer);
                events.add(new KernelEvent(
                        time, running.spec.processId(),
                        KernelEvent.Type.TIMER_INTERRUPT, "RUNNING -> READY"));
                requeueAfterAdmissions = running;
                running = null;
            }
        }

        Map<Integer, ProcessState> finalStates = new LinkedHashMap<>();
        processes.stream()
                .sorted(Comparator.comparingInt(p -> p.spec.processId()))
                .forEach(process -> finalStates.put(
                        process.spec.processId(), process.pcb.state()));
        return new KernelResult(
                time,
                timeline,
                events,
                interrupts,
                finalStates,
                dispatcher.history(),
                dispatcher.contextSwitches());
    }

    private static void appendTick(
            List<ExecutionSlice> timeline, int processId, int time) {
        if (!timeline.isEmpty()) {
            ExecutionSlice last = timeline.getLast();
            if (last.processId() == processId && last.endTime() == time) {
                timeline.set(
                        timeline.size() - 1,
                        new ExecutionSlice(processId, last.startTime(), last.duration() + 1));
                return;
            }
        }
        timeline.add(new ExecutionSlice(processId, time, 1));
    }

    private static void validate(List<KernelProcessSpec> workload) {
        Set<Integer> ids = new HashSet<>();
        for (KernelProcessSpec process : workload) {
            Objects.requireNonNull(process, "process");
            if (!ids.add(process.processId())) {
                throw new IllegalArgumentException(
                        "Duplicate process id: " + process.processId());
            }
        }
    }

    private static final class RuntimeProcess {
        private final KernelProcessSpec spec;
        private final ProcessControlBlock pcb;
        private int burstIndex;
        private int remainingInBurst;
        private int unblockAt = -1;
        private long readySequence;
        private boolean admitted;

        private RuntimeProcess(KernelProcessSpec spec) {
            this.spec = spec;
            pcb = new ProcessControlBlock(
                    spec.processId(),
                    spec.priority(),
                    new Program(Instruction.noOperands(Opcode.HALT)));
            remainingInBurst = spec.cpuBursts().getFirst();
        }

        private ProcessSnapshot snapshot() {
            return new ProcessSnapshot(
                    spec.processId(),
                    spec.arrivalTime(),
                    spec.totalCpuTime(),
                    remainingInBurst,
                    spec.priority(),
                    readySequence);
        }
    }
}
