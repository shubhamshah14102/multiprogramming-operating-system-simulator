# Phase 3 Workloads

The benchmark inputs are deterministic generator definitions committed under
`src/main/resources/workloads/`. The parser materializes each definition once,
then every compared policy receives that same immutable list.

## Scheduling

`scheduling.properties` defines exactly 20 processes and seed `20260314`.
Generation uses Java 21 `java.util.SplittableRandom`. For process IDs 1 through
20, it draws, in order:

1. arrival time uniformly from `[0, 30]`;
2. CPU burst uniformly from `[1, 12]`;
3. static priority uniformly from `[1, 5]` (lower is higher priority).

The policies compared are FCFS, non-preemptive SJF, Round Robin with quantum 4,
and non-preemptive static priority.

## Paging

`paging.properties` defines one trace of exactly 1,000 page references, seed
`20260315`, 12 virtual pages, and 4 physical frames. The `paging-locality-v1`
algorithm uses Java 21 `SplittableRandom`:

1. Choose the initial page uniformly from `[0, 11]`.
2. At every 100-reference phase boundary, choose a page uniformly from
   `[0, 11]`.
3. At every other reference, with 80% probability move by -1, 0, or +1
   uniformly, wrapping modulo 12; otherwise choose uniformly from `[0, 11]`.

FIFO, LRU, and offline Optimal all consume the same materialized trace and use
the same frame count.

## Reproduce

From the project root after compilation:

```text
java -cp target/classes os.benchmark.BenchmarkDriver --output results
```

Optional `--scheduling FILE` and `--paging FILE` arguments accept alternate
definition files with the same documented property schema.
