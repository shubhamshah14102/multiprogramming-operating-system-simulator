# Experiments

Inputs are generator files, not hand-typed result tables.

- Scheduling: `src/main/resources/workloads/scheduling.properties`
  - 20 processes, seed `20260314`
  - arrival `[0,30]`, burst `[1,12]`, priority `[1,5]` (lower is higher)
  - FCFS, SJF (non-preemptive), RR q=2/4/8, priority (non-preemptive)
- Paging: `src/main/resources/workloads/paging.properties`
  - 1,000 references, seed `20260315`, 12 pages, 4 frames
  - locality 80%, phase length 100
  - FIFO, LRU, Optimal on that exact list

```text
mvn -q -DskipTests compile
java -cp target/classes os.Main bench --output results
```

## What was measured

```text
Avg response
FCFS      #########################  49.8
SJF       ################           31.6
Priority  #######################    46.8
RR q=4    ##############             28.2
```

```mermaid
pie title Page faults
    "FIFO" : 397
    "LRU" : 396
    "Optimal" : 273
```

| Policy | Avg response | Switches |
| --- | ---: | ---: |
| FCFS | 49.800 | 18 |
| SJF | 31.600 | 18 |
| Priority | 46.800 | 18 |
| RR q=4 | 28.150 | 34 |

RR vs FCFS response: **43.5%** lower, more switches.

### Round Robin quantum sensitivity

| Quantum | Avg response | Avg wait | Avg turnaround | Switches |
| ---: | ---: | ---: | ---: | ---: |
| 2 | 15.300 | 62.600 | 68.900 | 63 |
| 4 | 28.150 | 58.600 | 64.900 | 34 |
| 8 | 44.850 | 52.450 | 58.750 | 21 |

For this workload, smaller quanta improve first response but increase
dispatching and waiting. The result is a tradeoff, not a universal best
quantum.

| Policy | Faults |
| --- | ---: |
| FIFO | 397 |
| LRU | 396 |
| Optimal | 273 |

Short-trace Gantt (3 jobs) is in the README.

Definitions: response = first dispatch − arrival; wait = turnaround − burst;
turnaround = completion − arrival; context switch = dispatcher change of
running pid; page fault includes compulsory misses.
