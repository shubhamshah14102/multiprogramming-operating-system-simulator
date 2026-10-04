package os.interrupt;

import org.junit.jupiter.api.Test;
import os.machine.ExecutionMode;
import os.machine.Instruction;
import os.machine.MachineMemory;
import os.machine.Opcode;
import os.machine.Program;
import os.machine.VirtualCpu;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InterruptDispatchTest {
    @Test
    void dispatchesServiceAndTimerInterruptsInMasterMode() {
        InterruptDispatcher dispatcher = new InterruptDispatcher();
        List<String> handled = new ArrayList<>();
        dispatcher.register(InterruptType.SERVICE, (interrupt, cpu) ->
                handled.add("service:" + interrupt.code() + ":" + cpu.mode()));
        dispatcher.register(InterruptType.TIMER, (interrupt, cpu) ->
                handled.add("timer:" + interrupt.code() + ":" + cpu.mode()));

        VirtualCpu cpu = new VirtualCpu(new MachineMemory(32), 2, dispatcher);
        cpu.loadProgram(new Program(
                Instruction.address(Opcode.SVC, 7),
                Instruction.noOperands(Opcode.NOP),
                Instruction.noOperands(Opcode.HALT)
        ));

        cpu.run(10);

        assertEquals(List.of("service:7:MASTER", "timer:2:MASTER"), handled);
        assertEquals(ExecutionMode.USER, cpu.mode());
        assertEquals(
                List.of(InterruptType.SERVICE, InterruptType.TIMER),
                dispatcher.history().stream().map(Interrupt::type).toList());
    }

    @Test
    void rejectsPrivilegedInstructionInUserMode() {
        InterruptDispatcher dispatcher = new InterruptDispatcher();
        VirtualCpu cpu = new VirtualCpu(new MachineMemory(16), 1, dispatcher);
        cpu.loadProgram(new Program(
                Instruction.address(Opcode.SET_TIMER, 5),
                Instruction.noOperands(Opcode.HALT)
        ));

        cpu.step();

        Interrupt interrupt = dispatcher.history().getFirst();
        assertEquals(1, dispatcher.history().size());
        assertEquals(InterruptType.PROGRAM, interrupt.type());
        assertEquals(ProgramFault.PRIVILEGE_VIOLATION, interrupt.fault().orElseThrow());
        assertEquals(0, cpu.retiredInstructions());
        assertFalse(cpu.isRunning());
    }

    @Test
    void reportsUnknownOpcodeAsProgramInterrupt() {
        InterruptDispatcher dispatcher = new InterruptDispatcher();
        VirtualCpu cpu = new VirtualCpu(new MachineMemory(16), 100, dispatcher);
        cpu.loadProgram(new Program(0x7F00_0000));

        cpu.step();

        Interrupt interrupt = dispatcher.history().getFirst();
        assertEquals(ProgramFault.ILLEGAL_INSTRUCTION, interrupt.fault().orElseThrow());
        assertFalse(cpu.isRunning());
    }
}
