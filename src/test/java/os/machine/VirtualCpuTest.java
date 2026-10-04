package os.machine;

import org.junit.jupiter.api.Test;
import os.interrupt.InterruptDispatcher;
import os.memory.FifoReplacementPolicy;
import os.memory.PagedVirtualMemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VirtualCpuTest {
    @Test
    void executesLoadAndStoreThroughDemandPaging() {
        PagedVirtualMemory memory =
                new PagedVirtualMemory(4, 8, 2, new FifoReplacementPolicy());
        VirtualCpu cpu = new VirtualCpu(memory, 100, new InterruptDispatcher());
        cpu.loadProgram(new Program(
                Instruction.registerImmediate(Opcode.LOAD_IMMEDIATE, 0, 42),
                Instruction.registerAddress(Opcode.STORE, 0, 8),
                Instruction.registerAddress(Opcode.LOAD, 1, 8),
                Instruction.noOperands(Opcode.HALT)));

        cpu.run(10);

        assertEquals(42, cpu.registers().read(1));
        assertEquals(2, memory.pageFaultCount());
        assertEquals(42, memory.read(8));
    }

    @Test
    void executesArithmeticMemoryAndBranchInstructionsDeterministically() {
        MachineMemory memory = new MachineMemory(64);
        VirtualCpu cpu = new VirtualCpu(memory, 100, new InterruptDispatcher());
        Program program = new Program(
                Instruction.registerImmediate(Opcode.LOAD_IMMEDIATE, 0, 7),
                Instruction.registerImmediate(Opcode.LOAD_IMMEDIATE, 1, 5),
                Instruction.registers(Opcode.ADD, 0, 1),
                Instruction.registerAddress(Opcode.STORE, 0, 20),
                Instruction.registerAddress(Opcode.LOAD, 2, 20),
                Instruction.registers(Opcode.SUBTRACT, 2, 1),
                Instruction.registerImmediate(Opcode.LOAD_IMMEDIATE, 3, 0),
                Instruction.registerAndAddress(Opcode.JUMP_IF_ZERO, 3, 9),
                Instruction.registerImmediate(Opcode.LOAD_IMMEDIATE, 2, 99),
                Instruction.noOperands(Opcode.HALT)
        );

        cpu.loadProgram(program);
        cpu.run(20);

        assertEquals(12, cpu.registers().read(0));
        assertEquals(7, cpu.registers().read(2));
        assertEquals(12, memory.read(20));
        assertEquals(10, cpu.registers().programCounter());
        assertEquals(9, cpu.retiredInstructions());
        assertFalse(cpu.isRunning());
    }

    @Test
    void programAndRegisterSnapshotsAreDefensiveCopies() {
        int[] words = {
                Instruction.noOperands(Opcode.NOP),
                Instruction.noOperands(Opcode.HALT)
        };
        Program program = new Program(words);
        words[0] = Instruction.noOperands(Opcode.HALT);

        assertEquals(Opcode.NOP, new InstructionDecoder().decode(program.wordAt(0)).opcode());

        RegisterFile registers = new RegisterFile();
        registers.write(0, 42);
        int[] snapshot = registers.snapshot();
        snapshot[0] = -1;
        assertEquals(42, registers.read(0));
    }
}
