# Architecture

Maven module. Entry point `os.Main`. Tests under `src/test/java`. Original
course files remain in `bin/` and are not compiled.

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

`SchedulingPolicy.select(ready, now)` is implemented by FCFS, non-preemptive
SJF, Round Robin, and non-preemptive priority. `SchedulerSimulator` is
event-driven. `MultiprogrammingKernel` is tick-driven and adds blocking I/O.

## Memory

`AddressTranslator` computes page and offset. `PagedVirtualMemory` handles
faults: pick a free frame or a victim (`FIFO` / `LRU` / `Optimal`), write
dirty pages to `BackingStore`, load the missing page.

## I/O

`BoundedBuffer`, `BlockingIoDevice`, `Spooler`. Spool drain does not drop a
request when the device queue is full.
