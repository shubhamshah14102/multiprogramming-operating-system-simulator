# Experiment results

Same 20-process workload for every scheduler. Same 1,000-reference
trace and 4 frames for every page-replacement policy.

## Scheduling

FCFS average response time 49.800, Round Robin (q=4) 28.150
(**43.474%** lower). Context switches: FCFS 18, RR 34.

## Paging

Faults, fewest to most: **OPTIMAL (273) < LRU (396) < FIFO (397)**.

## Reproduce

Seeds: scheduling `20260314`, paging `20260315`.
Definitions: `src/main/resources/workloads/`.
