package os.interrupt;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable interrupt request raised by the virtual CPU.
 */
public record Interrupt(
        InterruptType type,
        int code,
        ProgramFault programFault,
        String detail
) {
    public Interrupt {
        Objects.requireNonNull(type, "type");
        detail = detail == null ? "" : detail;
        if (type == InterruptType.PROGRAM && programFault == null) {
            throw new IllegalArgumentException("Program interrupts require a fault");
        }
        if (type != InterruptType.PROGRAM && programFault != null) {
            throw new IllegalArgumentException("Only program interrupts have a fault");
        }
    }

    public static Interrupt service(int serviceCode) {
        return new Interrupt(InterruptType.SERVICE, serviceCode, null, "");
    }

    public static Interrupt timer(long elapsedInstructions) {
        return new Interrupt(
                InterruptType.TIMER,
                Math.toIntExact(elapsedInstructions),
                null,
                "Timer quantum expired");
    }

    public static Interrupt io(int processId, String deviceName) {
        return new Interrupt(
                InterruptType.IO,
                processId,
                null,
                "I/O completed on " + Objects.requireNonNull(deviceName, "deviceName"));
    }

    public static Interrupt program(ProgramFault fault, String detail) {
        return new Interrupt(InterruptType.PROGRAM, fault.ordinal(), fault, detail);
    }

    public Optional<ProgramFault> fault() {
        return Optional.ofNullable(programFault);
    }
}
