# Experiment methodology and measured results

## Inputs (committed before interpretation)

- Scheduling: `src/main/resources/workloads/scheduling.properties`
  - 20 processes, seed `20260314`, arrivals in `[0,30]`, bursts in `[1,12]`,
    priorities in `[1,5]` (lower number is higher priority)
  - compared: FCFS, non-preemptive SJF, Round Robin quantum 4, non-preemptive priority
- Paging: `src/main/resources/workloads/paging.properties`
  - 1,000 references, seed `20260315`, 12 pages, 4 frames, locality 80%,
    phase length 100
  - compared: FIFO, LRU, Optimal (same trace and frame count)

Generator algorithms are documented in [`WORKLOADS.md`](../WORKLOADS.md).
Do not edit those files to chase a résumé percentage.

## How to reproduce

```text
mvn -q -DskipTests compile
java -cp target/classes os.Main bench --output results
```

or `scripts/demo.sh` / `scripts/demo.ps1`.

## Measured scheduling result

| Policy | Avg response | Avg waiting | Avg turnaround | Context switches |
| --- | ---: | ---: | ---: | ---: |
| FCFS | 49.800 | 49.800 | 56.100 | 18 |
| SJF (non-preemptive) | 31.600 | 31.600 | 37.900 | 18 |
| RR q=4 | 28.150 | 58.600 | 64.900 | 34 |
| Priority (non-preemptive) | 46.800 | 46.800 | 53.100 | 18 |

Round Robin reduced average response time by **43.474%** versus FCFS
(49.800 → 28.150) and used more context switches (18 → 34).

**Résumé claim of ~72% response-time reduction: not supported.** Use 43%.
The workload was not retuned to force 72%.

## Measured paging result

| Policy | Faults | Hits | Fault rate |
| --- | ---: | ---: | ---: |
| FIFO | 397 | 603 | 39.7% |
| LRU | 396 | 604 | 39.6% |
| Optimal | 273 | 727 | 27.3% |

Faults decreased FIFO → LRU → Optimal on this trace, but LRU beat FIFO by
**one** fault. That is too thin to describe as a consistent LRU advantage.
Optimal is clearly better.

FIFO is not universally worse than LRU. A short counter-example with 3 frames
and reference string `1 2 3 4 1 2 5 1 2` is a standard teaching case where
FIFO can fault less than LRU; the unit tests cover short traces separately.

## Metric definitions

- Response time: first dispatch time − arrival time
- Waiting time: turnaround − CPU burst
- Turnaround: completion − arrival
- Context switch: dispatcher change from one process id to another (idle is
  not counted as a process-to-process switch)
- Page fault: first mapping of a page into a frame, including compulsory misses
