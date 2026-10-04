# Experiments

Inputs are generator files, not hand-typed result tables.

- Scheduling: `src/main/resources/workloads/scheduling.properties`
  - 20 processes, seed `20260314`
  - arrival `[0,30]`, burst `[1,12]`, priority `[1,5]` (lower is higher)
  - FCFS, SJF (non-preemptive), RR q=4, priority (non-preemptive)
- Paging: `src/main/resources/workloads/paging.properties`
  - 1,000 references, seed `20260315`, 12 pages, 4 frames
  - locality 80%, phase length 100
  - FIFO, LRU, Optimal on that exact list

```text
mvn -q -DskipTests compile
java -cp target/classes os.Main bench --output results
```

## What was measured

```mermaid
xychart-beta
    title Avg response (20 processes)
    x-axis [FCFS, SJF, Priority, RR]
    y-axis 0 --> 55
    bar [49.8, 31.6, 46.8, 28.15]
```

```mermaid
xychart-beta
    title Page faults (1000 refs, 4 frames)
    x-axis [FIFO, LRU, Optimal]
    y-axis 0 --> 450
    bar [397, 396, 273]
```

| Policy | Avg response | Switches |
| --- | ---: | ---: |
| FCFS | 49.800 | 18 |
| SJF | 31.600 | 18 |
| Priority | 46.800 | 18 |
| RR q=4 | 28.150 | 34 |

RR vs FCFS response: **43.5%** lower, more switches.

| Policy | Faults |
| --- | ---: |
| FIFO | 397 |
| LRU | 396 |
| Optimal | 273 |

Short-trace Gantt (3 jobs) is in the README.

Definitions: response = first dispatch − arrival; wait = turnaround − burst;
turnaround = completion − arrival; context switch = dispatcher change of
running pid; page fault includes compulsory misses.
