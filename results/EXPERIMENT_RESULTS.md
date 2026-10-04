# Phase 3 Experiment Results

These are measured simulator results, not target values. All scheduling
policies used the same seeded 20-process workload. All paging policies
used the same seeded 1,000-reference trace and the same 4 frames.

## Scheduling finding

FCFS average response time was 49.800 ticks and RR quantum 4 average
response time was 28.150 ticks. The measured RR-vs-FCFS reduction was
**43.474%**. Context switches were **18 for FCFS** and **34 for RR**.

## Paging finding

Fault ordering from fewest to most was **OPTIMAL (273) < LRU (396) < FIFO (397)**. The number in
parentheses is the actual page-fault count.

## Résumé-claim check

A résumé claim of exactly 72% lower response time is **NOT SUPPORTED** by this
experiment. The defensible claim is the measured 43.474% reduction above;
the workload was not tuned to force a target percentage.

## Reproduction inputs

Scheduling seed: `20260314`. Paging seed: `20260315`. The complete generator
configuration is committed in `src/main/resources/workloads/` and
documented in `WORKLOADS.md`.
