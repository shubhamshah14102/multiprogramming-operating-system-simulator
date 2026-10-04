package os.machine;

import os.interrupt.Interrupt;
import os.interrupt.InterruptDispatcher;
import os.interrupt.InterruptType;
import os.interrupt.ProgramFault;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * A deterministic virtual CPU with an explicit fetch-decode-execute cycle.
 */
public final class VirtualCpu {
    private final Memory memory;
    private final RegisterFile registers;
    private final InstructionDecoder decoder;
    private final InterruptDispatcher interruptDispatcher;
    private final Deque<Interrupt> pendingInterrupts = new ArrayDeque<>();

    private ExecutionMode mode = ExecutionMode.MASTER;
    private boolean running;
    private int programSize;
    private int timerQuantum;
    private int instructionsSinceTimer;
    private long retiredInstructions;

    public VirtualCpu(
            Memory memory,
            int timerQuantum,
            InterruptDispatcher interruptDispatcher
    ) {
        this.memory = Objects.requireNonNull(memory, "memory");
        this.interruptDispatcher =
                Objects.requireNonNull(interruptDispatcher, "interruptDispatcher");
        this.registers = new RegisterFile();
        this.decoder = new InstructionDecoder();
        setTimerQuantum(timerQuantum);
    }

    public void loadProgram(Program program) {
        Objects.requireNonNull(program, "program");
        if (program.size() > memory.size()) {
            throw new IllegalArgumentException("Program does not fit in memory");
        }

        memory.clear();
        int[] words = program.words();
        for (int address = 0; address < words.length; address++) {
            memory.write(address, words[address]);
        }

        registers.clear();
        pendingInterrupts.clear();
        programSize = words.length;
        instructionsSinceTimer = 0;
        retiredInstructions = 0;
        mode = ExecutionMode.USER;
        running = true;
    }

    /**
     * Performs one complete fetch-decode-execute cycle and dispatches all
     * interrupts raised by that cycle.
     */
    public void step() {
        if (!running) {
            throw new IllegalStateException("CPU is not running");
        }

        try {
            int word = fetch();
            DecodedInstruction instruction = decode(word);
            execute(instruction);
            boolean programFaultRaised = pendingInterrupts.stream()
                    .anyMatch(interrupt -> interrupt.type() == InterruptType.PROGRAM);
            if (!programFaultRaised) {
                retiredInstructions++;
                instructionsSinceTimer++;
                if (running && instructionsSinceTimer >= timerQuantum) {
                    pendingInterrupts.add(Interrupt.timer(instructionsSinceTimer));
                    instructionsSinceTimer = 0;
                }
            }
        } catch (IndexOutOfBoundsException exception) {
            pendingInterrupts.add(
                    Interrupt.program(ProgramFault.ADDRESS_ERROR, exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            pendingInterrupts.add(
                    Interrupt.program(ProgramFault.ILLEGAL_INSTRUCTION, exception.getMessage()));
        }

        dispatchPendingInterrupts();
    }

    public void run(int maximumInstructions) {
        if (maximumInstructions <= 0) {
            throw new IllegalArgumentException("Maximum instructions must be positive");
        }
        int executed = 0;
        while (running && executed < maximumInstructions) {
            step();
            executed++;
        }
        if (running) {
            throw new IllegalStateException(
                    "Execution limit reached after " + maximumInstructions + " instructions");
        }
    }

    public int fetch() {
        int address = registers.fetchAddressAndIncrement();
        if (address >= programSize) {
            throw new IndexOutOfBoundsException(
                    "Instruction address out of range: " + address);
        }
        return memory.read(address);
    }

    public DecodedInstruction decode(int word) {
        return decoder.decode(word);
    }

    public void execute(DecodedInstruction instruction) {
        switch (instruction.opcode()) {
            case NOP -> { }
            case LOAD_IMMEDIATE ->
                    registers.write(instruction.registerA(), instruction.immediate());
            case LOAD ->
                    registers.write(
                            instruction.registerA(), memory.read(instruction.address()));
            case STORE ->
                    memory.write(
                            instruction.address(), registers.read(instruction.registerA()));
            case MOVE ->
                    registers.write(
                            instruction.registerA(), registers.read(instruction.registerB()));
            case ADD ->
                    registers.write(
                            instruction.registerA(),
                            registers.read(instruction.registerA())
                                    + registers.read(instruction.registerB()));
            case SUBTRACT ->
                    registers.write(
                            instruction.registerA(),
                            registers.read(instruction.registerA())
                                    - registers.read(instruction.registerB()));
            case JUMP -> registers.setProgramCounter(instruction.address());
            case JUMP_IF_ZERO -> {
                if (registers.read(instruction.registerA()) == 0) {
                    registers.setProgramCounter(instruction.address());
                }
            }
            case SVC -> pendingInterrupts.add(Interrupt.service(instruction.address()));
            case HALT -> halt();
            case SET_TIMER -> {
                if (mode != ExecutionMode.MASTER) {
                    pendingInterrupts.add(Interrupt.program(
                            ProgramFault.PRIVILEGE_VIOLATION,
                            "SET_TIMER is restricted to master mode"));
                } else {
                    setTimerQuantum(instruction.address());
                }
            }
        }
    }

    private void dispatchPendingInterrupts() {
        while (!pendingInterrupts.isEmpty()) {
            interruptDispatcher.dispatch(pendingInterrupts.removeFirst(), this);
        }
    }

    public RegisterFile registers() {
        return registers;
    }

    public Memory memory() {
        return memory;
    }

    public ExecutionMode mode() {
        return mode;
    }

    public void setMode(ExecutionMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public void enterMasterMode() {
        mode = ExecutionMode.MASTER;
    }

    public void halt() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public long retiredInstructions() {
        return retiredInstructions;
    }

    public int timerQuantum() {
        return timerQuantum;
    }

    public void setTimerQuantum(int timerQuantum) {
        if (timerQuantum <= 0) {
            throw new IllegalArgumentException("Timer quantum must be positive");
        }
        this.timerQuantum = timerQuantum;
        this.instructionsSinceTimer = 0;
    }
}
