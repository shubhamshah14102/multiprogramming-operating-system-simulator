package os.process;

import os.machine.RegisterFile;

import java.util.Arrays;

/**
 * Immutable register snapshot stored in a process control block.
 */
public record CpuContext(int programCounter, int[] registers) {
    public CpuContext {
        if (programCounter < 0) {
            throw new IllegalArgumentException("Program counter cannot be negative");
        }
        if (registers.length != RegisterFile.GENERAL_REGISTER_COUNT) {
            throw new IllegalArgumentException("Expected eight registers");
        }
        registers = Arrays.copyOf(registers, registers.length);
    }

    @Override
    public int[] registers() {
        return Arrays.copyOf(registers, registers.length);
    }

    public static CpuContext empty() {
        return new CpuContext(0, new int[RegisterFile.GENERAL_REGISTER_COUNT]);
    }
}
