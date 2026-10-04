package os.process;

import os.machine.Program;
import os.machine.RegisterFile;

import java.util.Objects;

/**
 * Kernel-owned process metadata and saved CPU state.
 */
public final class ProcessControlBlock {
    private final int processId;
    private final int priority;
    private final Program program;
    private ProcessState state = ProcessState.NEW;
    private CpuContext context = CpuContext.empty();

    public ProcessControlBlock(int processId, int priority, Program program) {
        if (processId < 0) {
            throw new IllegalArgumentException("Process id cannot be negative");
        }
        this.processId = processId;
        this.priority = priority;
        this.program = Objects.requireNonNull(program, "program");
    }

    public void transitionTo(ProcessState nextState) {
        Objects.requireNonNull(nextState, "nextState");
        boolean valid = switch (state) {
            case NEW -> nextState == ProcessState.READY
                    || nextState == ProcessState.TERMINATED;
            case READY -> nextState == ProcessState.RUNNING
                    || nextState == ProcessState.TERMINATED;
            case RUNNING -> nextState == ProcessState.READY
                    || nextState == ProcessState.BLOCKED
                    || nextState == ProcessState.TERMINATED;
            case BLOCKED -> nextState == ProcessState.READY
                    || nextState == ProcessState.TERMINATED;
            case TERMINATED -> false;
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Invalid process transition: " + state + " -> " + nextState);
        }
        state = nextState;
    }

    public void saveContext(RegisterFile registers) {
        Objects.requireNonNull(registers, "registers");
        context = new CpuContext(registers.programCounter(), registers.snapshot());
    }

    public void restoreContext(RegisterFile registers) {
        Objects.requireNonNull(registers, "registers");
        registers.restore(context.programCounter(), context.registers());
    }

    public int processId() {
        return processId;
    }

    public int priority() {
        return priority;
    }

    public Program program() {
        return program;
    }

    public ProcessState state() {
        return state;
    }

    public CpuContext context() {
        return context;
    }
}
