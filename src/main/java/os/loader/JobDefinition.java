package os.loader;

import os.machine.Program;

import java.util.Objects;

public record JobDefinition(int processId, int priority, Program program) {
    public JobDefinition {
        if (processId < 0) {
            throw new IllegalArgumentException("Process id cannot be negative");
        }
        Objects.requireNonNull(program, "program");
    }
}
