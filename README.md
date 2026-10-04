# Multiprogramming Operating System Simulator

Java teaching simulator with an explicit virtual CPU, interrupts, processes,
CPU scheduling, demand-paged memory, and spooled I/O. Experiments compare
four schedulers and three page-replacement policies on committed workloads.

## What it does

A user job is parsed into words, loaded into a PCB, and executed on a
word-addressable virtual machine. A kernel loop can multiplex several
processes. A separate paged-memory subsystem translates addresses and
handles faults. A benchmark driver runs the same 20-process workload and the
same 1,000-reference trace through interchangeable algorithms and writes
JSON/CSV.

## Implemented from first principles

Scheduling decisions, page replacement, address translation, interrupt
dispatch, and process-state transitions are written in this repository. Java
collections hold queues; they do not hide the OS policy.

## Architecture

```text
JobLoader -> PCB -> Ready queue -> Scheduler -> Dispatcher -> VirtualCPU
                                              -> MMU / page table
                                              -> Demand pager / replacement
                                              -> Swap / backing store
VirtualCPU -> Interrupt controller -> Kernel loop
VirtualCPU -> Spool / bounded buffer -> Device
Kernel loop -> Metrics
```

Details: [`docs/design.md`](docs/design.md).

## Simulated ISA / execution model

32-bit instructions: opcode in bits 31..24, registers in 23..16, address or
immediate in 15..0. Registers `R0`–`R7` plus a program counter.
Mnemonics: `LOADI`, `LOAD`, `STORE`, `MOVE`, `ADD`, `SUB`, `JUMP`, `JZ`,
`SVC`, `HALT`, `SET_TIMER`. One `step()` is one fetch-decode-execute cycle.

## Process lifecycle

`NEW → READY → RUNNING`, then `READY` (preempt), `BLOCKED` (I/O), or
`TERMINATED`. The PCB stores pid, priority, program, and saved CPU context.

## Interrupt model

Service (`SVC`), timer, program faults, and I/O completion. Dispatch enters
master mode, runs the handler, then restores the interrupted mode unless the
handler halted the CPU.

## Memory-management model

`virtual = page * pageSize + offset`. Missing pages fault, a replacement
policy picks a frame, dirty pages write back, then the page is loaded from
the backing store.

## Scheduling algorithms

FCFS, non-preemptive SJF, Round Robin, non-preemptive priority. Same
`SchedulingPolicy` interface and the same process list.

## Page-replacement algorithms

FIFO, LRU, and offline Optimal. Optimal is a trace oracle, not a runtime OS
policy.

## Build, run, test

Requires JDK 21 and Maven.

```text
mvn test
mvn -q -DskipTests compile
java -cp target/classes os.Main job src/main/resources/jobs/add.job
java -cp target/classes os.Main bench --output results
```

Demo scripts: `scripts/demo.sh`, `scripts/demo.ps1`.

CI: `.github/workflows/tests.yml` runs `mvn test` on Java 21.

## Reproducible experiments

Workload generators: `src/main/resources/workloads/` (see `WORKLOADS.md`).
The 20-process and 1,000-reference experiments are defined there, not typed
into reports by hand.

## Benchmark results (measured)

On the committed inputs:

- Round Robin (q=4) average response **28.150** vs FCFS **49.800**
  (**43.474%** reduction), context switches **34** vs **18**
- Page faults on the 1,000-reference / 4-frame trace:
  FIFO **397**, LRU **396**, Optimal **273**

A résumé line claiming ~72% Round-Robin response improvement is **not**
supported by this experiment. Full write-up: [`docs/experiments.md`](docs/experiments.md)
and [`results/EXPERIMENT_RESULTS.md`](results/EXPERIMENT_RESULTS.md).

## Limitations

- The virtual ISA is a 2026 teaching design; the 2022 labs had no opcode sheet
- Optimal replacement sees the future trace
- The kernel loop uses scripted CPU/I/O bursts; it is not a full multi-user OS
- LRU vs FIFO on the long trace differs by one fault; do not over-claim LRU
- Banker's algorithm and mutex/semaphore programs stay in the 2022 lab archive

## Original academic context versus later portfolio work

| Year | What |
| --- | --- |
| 2022 | Discrete labs uploaded in one commit to `bin/` (Linux/shell PDFs, scheduling, paging, sync). Classmate folders are preserved and labeled. |
| 2026 | Integrated simulator in `src/`, tests, workloads, and measured results |

The PDFs in `bin/_Linux/_Shubham/` are Linux command and shell-script
assignments, not the specification for this simulator.
