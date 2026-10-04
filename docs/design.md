# Architecture

Maven module. Entry point `os.Main`. Tests under `src/test/java`. Original
course files remain in `bin/` and are not compiled.

## Execution paths

The repository has two deliberate paths:

1. `os.Main job` loads a text program into a `VirtualCpu` backed by
   `PagedVirtualMemory`. Instruction fetches and user memory operations use
   demand paging.
2. `MultiprogrammingKernel` exercises admission, scheduling, preemption, and
   blocking with scripted CPU/I/O bursts. It does not execute the ISA.

The benchmark driver evaluates scheduling and page replacement separately on
shared, seeded inputs.

## CPU

`VirtualCpu` fetches a word, decodes opcode/registers/immediate, executes.
Eight GPRs + PC. `USER` / `MASTER`. Timer ticks in instruction counts, not
wall-clock time.

## Interrupts

`InterruptDispatcher` switches to master, records the interrupt, runs the
handler, restores the previous mode unless the CPU halted.

## Processes

`ProcessControlBlock` stores identity, program, state, and a register
snapshot. `JobParser` reads the text format used in `src/main/resources/jobs/`.

## Scheduling

`SchedulingPolicy.select(ready, now)`:

- FCFS — oldest ready
- SJF — shortest remaining burst (non-preemptive)
- Priority — lowest number (non-preemptive)
- RR — oldest ready, stop after `q` ticks

`SchedulerSimulator` is event-driven. `MultiprogrammingKernel` is tick-driven
and adds blocking I/O.

## Memory

`AddressTranslator` computes page and offset. `PagedVirtualMemory` handles
faults: pick a free frame or a victim (`FIFO` / `LRU` / `Optimal`), write
dirty pages to `BackingStore`, load the missing page. This is page write-back,
not whole-process swap.

## I/O

`BoundedBuffer`, `BlockingIoDevice`, `Spooler`. Spool drain does not drop a
request when the device queue is full.
