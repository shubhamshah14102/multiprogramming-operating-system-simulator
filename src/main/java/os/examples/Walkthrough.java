package os.examples;

import os.interrupt.InterruptDispatcher;
import os.loader.JobLoader;
import os.loader.JobParser;
import os.machine.DecodedInstruction;
import os.machine.MachineMemory;
import os.machine.VirtualCpu;
import os.memory.AddressTranslation;
import os.memory.AddressTranslator;
import os.memory.FifoReplacementPolicy;
import os.memory.LruReplacementPolicy;
import os.memory.OptimalReplacementPolicy;
import os.memory.PageReplacementPolicy;
import os.memory.PageTable;
import os.memory.PagedVirtualMemory;
import os.process.ProcessControlBlock;
import os.process.ProcessState;
import os.scheduling.ExecutionSlice;
import os.scheduling.FcfsPolicy;
import os.scheduling.PriorityPolicy;
import os.scheduling.ProcessSpec;
import os.scheduling.RoundRobinPolicy;
import os.scheduling.ScheduleResult;
import os.scheduling.SchedulerSimulator;
import os.scheduling.SjfPolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Prints short, deterministic traces used in the README. */
public final class Walkthrough {
    public static final List<ProcessSpec> DEMO_PROCESSES = List.of(
            new ProcessSpec(1, 0, 5, 2),
            new ProcessSpec(2, 1, 2, 1),
            new ProcessSpec(3, 1, 1, 0));

    public static final List<Integer> DEMO_PAGES =
            List.of(7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1);

    private Walkthrough() {
    }

    public static void write(Appendable out, Path jobFile) throws IOException {
        cpuTrace(out, jobFile);
        translation(out);
        schedules(out);
        paging(out);
    }

    private static void cpuTrace(Appendable out, Path jobFile) throws IOException {
        String source = Files.readString(jobFile, StandardCharsets.UTF_8);
        ProcessControlBlock pcb =
                new JobLoader().load(new JobParser().parse(source)).getFirst();
        MachineMemory memory = new MachineMemory(256);
        VirtualCpu cpu = new VirtualCpu(memory, 64, new InterruptDispatcher());
        cpu.loadProgram(pcb.program());
        pcb.transitionTo(ProcessState.READY);
        pcb.transitionTo(ProcessState.RUNNING);

        out.append("CPU trace (").append(jobFile.toString().replace('\\', '/'))
                .append(")\n");
        while (cpu.isRunning()) {
            int pc = cpu.registers().programCounter();
            DecodedInstruction decoded = cpu.decode(cpu.memory().read(pc));
            cpu.step();
            out.append(String.format(
                    "  pc=%d %s r0=%d%n",
                    pc, decoded.opcode(), cpu.registers().read(0)));
        }
        pcb.saveContext(cpu.registers());
        pcb.transitionTo(ProcessState.TERMINATED);
        out.append(String.format(
                "  halt pid=%d pc=%d mem[20]=%d%n%n",
                pcb.processId(), cpu.registers().programCounter(), memory.read(20)));
    }

    private static void translation(Appendable out) throws IOException {
        PageTable table = new PageTable(4);
        table.map(2, 1);
        AddressTranslation t = new AddressTranslator(4, table).translate(10);
        out.append("Address translation  VA 10, page size 4, PTE[2]=frame 1\n");
        out.append(String.format(
                "  page=%d offset=%d frame=%d PA=%d%n%n",
                t.pageNumber(), t.offset(), t.frameNumber(), t.physicalAddress()));
    }

    private static void schedules(Appendable out) throws IOException {
        out.append("Schedulers on P1(at=0,bt=5,pr=2) P2(1,2,1) P3(1,1,0)\n");
        printSchedule(out, "FCFS", new SchedulerSimulator(new FcfsPolicy()).run(DEMO_PROCESSES));
        printSchedule(out, "SJF", new SchedulerSimulator(new SjfPolicy()).run(DEMO_PROCESSES));
        printSchedule(out, "PRIORITY", new SchedulerSimulator(new PriorityPolicy()).run(DEMO_PROCESSES));
        printSchedule(out, "RR q=2", new SchedulerSimulator(new RoundRobinPolicy(2)).run(DEMO_PROCESSES));
        out.append('\n');
    }

    private static void printSchedule(
            Appendable out, String name, ScheduleResult result) throws IOException {
        StringBuilder line = new StringBuilder("  ").append(name).append(':');
        for (ExecutionSlice slice : result.timeline()) {
            line.append(' ')
                    .append(slice.processId() < 0 ? "idle" : "P" + slice.processId())
                    .append('[')
                    .append(slice.startTime())
                    .append(',')
                    .append(slice.endTime())
                    .append(')');
        }
        line.append("  switches=").append(result.contextSwitches()).append('\n');
        out.append(line);
    }

    private static void paging(Appendable out) throws IOException {
        out.append("Page replacement, 3 frames, refs ")
                .append(DEMO_PAGES.toString())
                .append('\n');
        runPolicy(out, new FifoReplacementPolicy());
        runPolicy(out, new LruReplacementPolicy());
        runPolicy(out, new OptimalReplacementPolicy(DEMO_PAGES));
    }

    private static void runPolicy(Appendable out, PageReplacementPolicy policy)
            throws IOException {
        PagedVirtualMemory memory = new PagedVirtualMemory(8, 1, 3, policy);
        DEMO_PAGES.forEach(memory::read);
        out.append(String.format(
                "  %s faults=%d evictions=%d%n",
                policy.name(),
                memory.pageFaultCount(),
                memory.pageFaultHistory().stream()
                        .filter(event -> event.evictedPage() != null)
                        .count()));
    }
}
