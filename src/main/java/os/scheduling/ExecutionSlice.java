package os.scheduling;

/**
 * A contiguous CPU interval. processId -1 is idle.
 */
public record ExecutionSlice(int processId, int startTime, int duration) {
    public static final int IDLE = -1;

    public ExecutionSlice {
        if (processId < IDLE || startTime < 0 || duration <= 0) {
            throw new IllegalArgumentException("Invalid execution slice");
        }
    }

    public int endTime() {
        return startTime + duration;
    }

    public boolean idle() {
        return processId == IDLE;
    }
}
