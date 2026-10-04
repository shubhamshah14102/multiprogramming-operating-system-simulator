package os.machine;

import java.util.Arrays;

/**
 * Eight general-purpose registers plus an explicit program counter.
 */
public final class RegisterFile {
    public static final int GENERAL_REGISTER_COUNT = 8;

    private final int[] general = new int[GENERAL_REGISTER_COUNT];
    private int programCounter;

    public int read(int index) {
        requireIndex(index);
        return general[index];
    }

    public void write(int index, int value) {
        requireIndex(index);
        general[index] = value;
    }

    public int programCounter() {
        return programCounter;
    }

    public void setProgramCounter(int programCounter) {
        if (programCounter < 0) {
            throw new IllegalArgumentException("Program counter cannot be negative");
        }
        this.programCounter = programCounter;
    }

    public int fetchAddressAndIncrement() {
        return programCounter++;
    }

    public int[] snapshot() {
        return Arrays.copyOf(general, general.length);
    }

    public void restore(int programCounter, int[] values) {
        if (values.length != GENERAL_REGISTER_COUNT) {
            throw new IllegalArgumentException("Expected eight general-purpose registers");
        }
        setProgramCounter(programCounter);
        System.arraycopy(values, 0, general, 0, general.length);
    }

    public void clear() {
        Arrays.fill(general, 0);
        programCounter = 0;
    }

    private static void requireIndex(int index) {
        if (index < 0 || index >= GENERAL_REGISTER_COUNT) {
            throw new IllegalArgumentException("Register index must be between 0 and 7");
        }
    }
}
