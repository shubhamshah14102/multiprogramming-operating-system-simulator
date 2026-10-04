# Workloads

Each policy sees the same list. `SplittableRandom` (Java 21) with the seed
in the properties file.

## Scheduling (`scheduling.properties`)

For pid 1..20, draw arrival, burst, then priority.

## Paging (`paging.properties`)

`paging-locality-v1`:

1. Start at a uniform page in `[0, page.count)`.
2. Every `phase.length` references, jump to a uniform page.
3. Otherwise with `locality.percent` walk −1/0/+1 (mod page count);
   else jump uniformly.

```text
java -cp target/classes os.Main bench --output results
```
