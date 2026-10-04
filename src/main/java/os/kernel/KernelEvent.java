package os.kernel;

public record KernelEvent(int time, int processId, Type type, String detail) {
    public enum Type {
        ARRIVAL,
        DISPATCH,
        TIMER_INTERRUPT,
        IO_REQUEST,
        IO_COMPLETION_INTERRUPT,
        TERMINATION,
        IDLE
    }
}
