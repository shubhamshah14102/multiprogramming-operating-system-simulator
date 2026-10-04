package os;

import os.benchmark.BenchmarkDriver;
import os.examples.Walkthrough;
import os.interrupt.InterruptDispatcher;
import os.loader.JobLoader;
import os.loader.JobParser;
import os.machine.MachineMemory;
import os.machine.VirtualCpu;
import os.process.ProcessControlBlock;
import os.process.ProcessState;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || "help".equals(args[0]) || "--help".equals(args[0])) {
            System.out.print("""
                    usage:
                      java -cp target/classes os.Main job <file.job>
                      java -cp target/classes os.Main walkthrough [file.job]
                      java -cp target/classes os.Main bench [--output DIR]
                    """);
            return;
        }
        switch (args[0]) {
            case "job" -> {
                if (args.length != 2) {
                    throw new IllegalArgumentException("usage: os.Main job <file.job>");
                }
                runJob(Path.of(args[1]));
            }
            case "walkthrough" -> {
                Path job = args.length == 2
                        ? Path.of(args[1])
                        : Path.of("src/main/resources/jobs/add.job");
                Walkthrough.write(System.out, job);
            }
            case "bench" -> {
                String[] rest = new String[args.length - 1];
                System.arraycopy(args, 1, rest, 0, rest.length);
                BenchmarkDriver.main(rest);
            }
            default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
        }
    }

    private static void runJob(Path path) throws Exception {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        List<ProcessControlBlock> processes =
                new JobLoader().load(new JobParser().parse(source));
        if (processes.size() != 1) {
            throw new IllegalArgumentException("job command runs exactly one JOB block");
        }
        ProcessControlBlock pcb = processes.getFirst();
        MachineMemory memory = new MachineMemory(256);
        VirtualCpu cpu = new VirtualCpu(memory, 64, new InterruptDispatcher());
        cpu.loadProgram(pcb.program());
        pcb.transitionTo(ProcessState.READY);
        pcb.transitionTo(ProcessState.RUNNING);
        cpu.run(1_000);
        pcb.saveContext(cpu.registers());
        pcb.transitionTo(ProcessState.TERMINATED);
        System.out.printf(
                "pid=%d halted pc=%d r0=%d mem[20]=%d retired=%d%n",
                pcb.processId(),
                cpu.registers().programCounter(),
                cpu.registers().read(0),
                memory.read(20),
                cpu.retiredInstructions());
    }
}
