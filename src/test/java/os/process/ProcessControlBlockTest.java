package os.process;

import org.junit.jupiter.api.Test;
import os.machine.Instruction;
import os.machine.Opcode;
import os.machine.Program;
import os.machine.RegisterFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessControlBlockTest {
    private final ProcessControlBlock process = new ProcessControlBlock(
            11, 3, new Program(Instruction.noOperands(Opcode.HALT)));

    @Test
    void enforcesProcessStateTransitions() {
        process.transitionTo(ProcessState.READY);
        process.transitionTo(ProcessState.RUNNING);
        process.transitionTo(ProcessState.BLOCKED);
        process.transitionTo(ProcessState.READY);
        process.transitionTo(ProcessState.RUNNING);
        process.transitionTo(ProcessState.TERMINATED);

        assertEquals(ProcessState.TERMINATED, process.state());
        assertThrows(
                IllegalStateException.class,
                () -> process.transitionTo(ProcessState.READY));
    }

    @Test
    void savesAndRestoresCpuContext() {
        RegisterFile registers = new RegisterFile();
        registers.write(2, 91);
        registers.setProgramCounter(4);
        process.saveContext(registers);

        registers.clear();
        process.restoreContext(registers);

        assertEquals(4, registers.programCounter());
        assertEquals(91, registers.read(2));

        int[] exposed = process.context().registers();
        exposed[2] = -10;
        assertEquals(91, process.context().registers()[2]);
    }
}
