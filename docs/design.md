# Design notes (2026 simulator)

This document describes the 2026 reconstruction. It is not a 2022 lab handout.

## Original 2022 context versus this code

The December 2022 GitHub commit is a dump of discrete labs: Linux/shell
assignments (including the PDFs under `bin/_Linux/_Shubham/`), standalone
scheduling and page-replacement programs, mutex/semaphore exercises, and
classmate files. There is no curriculum PDF that specifies a virtual machine
ISA. The machine below is a documented 2026 teaching design that makes the
OS mechanisms named on the résumé observable.

Original labs remain in [`bin/`](../bin/) and are also indexed in
[`historical/2022-os-labs/README.md`](../historical/2022-os-labs/README.md).

## Machine

- 32-bit words, eight general registers `R0`–`R7`, explicit program counter
- Instruction layout: opcode 31..24, reg A 23..20, reg B 19..16, imm/addr 15..0
- Opcodes: `NOP`, `LOADI`, `LOAD`, `STORE`, `MOVE`, `ADD`, `SUB`, `JUMP`,
  `JZ`, `SVC`, `HALT`, `SET_TIMER`
- Fetch-decode-execute is one `VirtualCpu.step()`
- `USER` versus `MASTER` mode; interrupt dispatch always enters master mode

## Interrupts

- Service (`SVC`)
- Timer (quantum)
- Program (illegal instruction, privilege, address)
- I/O completion (kernel wake-up)

## Processes

PCB fields: pid, priority, program, state (`NEW`/`READY`/`RUNNING`/`BLOCKED`/`TERMINATED`),
saved CPU context. Transitions are checked, not implied.

## Scheduling

`SchedulingPolicy` implementations: FCFS, non-preemptive SJF, Round Robin,
non-preemptive priority. `SchedulerSimulator` and `MultiprogrammingKernel`
share the same policy interface. The dispatcher counts process-to-process
switches.

## Memory

Virtual address = `page * pageSize + offset`. A miss is a page fault, the
chosen replacement policy selects a victim frame, dirty pages write back to
the backing store, then the missing page is loaded. FIFO and LRU are online.
Optimal is an offline oracle that receives the full trace.

## I/O

A bounded buffer, a blocking device queue, and a spool that will not drop a
job when the device is full (`drainOneTo` leaves the job in the spool).

## Experiments

Generator definitions live in `src/main/resources/workloads/`. Reports are
written by `os.benchmark.BenchmarkDriver`. Numbers in `results/` come from
that program. They are not résumé targets.
