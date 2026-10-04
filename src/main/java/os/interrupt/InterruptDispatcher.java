package os.interrupt;

import os.machine.ExecutionMode;
import os.machine.VirtualCpu;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Dispatches interrupts in master mode, then restores the previous mode. */
public final class InterruptDispatcher {
    private final Map<InterruptType, InterruptHandler> handlers =
            new EnumMap<>(InterruptType.class);
    private final List<Interrupt> history = new ArrayList<>();

    public InterruptDispatcher() {
        handlers.put(InterruptType.SERVICE, (interrupt, cpu) -> { });
        handlers.put(InterruptType.TIMER, (interrupt, cpu) -> { });
        handlers.put(InterruptType.PROGRAM, (interrupt, cpu) -> cpu.halt());
        handlers.put(InterruptType.IO, (interrupt, cpu) -> { });
    }

    public void register(InterruptType type, InterruptHandler handler) {
        handlers.put(Objects.requireNonNull(type), Objects.requireNonNull(handler));
    }

    public void dispatch(Interrupt interrupt, VirtualCpu cpu) {
        Objects.requireNonNull(interrupt, "interrupt");
        Objects.requireNonNull(cpu, "cpu");

        ExecutionMode interruptedMode = cpu.mode();
        cpu.enterMasterMode();
        history.add(interrupt);
        try {
            handlers.get(interrupt.type()).handle(interrupt, cpu);
        } finally {
            cpu.setMode(interruptedMode);
        }
    }

    public List<Interrupt> history() {
        return List.copyOf(history);
    }

    @FunctionalInterface
    public interface InterruptHandler {
        void handle(Interrupt interrupt, VirtualCpu cpu);
    }
}
