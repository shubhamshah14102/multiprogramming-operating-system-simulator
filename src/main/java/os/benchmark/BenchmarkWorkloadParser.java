package os.benchmark;

import os.scheduling.ProcessSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.SplittableRandom;

/** Parses and materializes the committed, seeded Phase 3 workload definitions. */
public final class BenchmarkWorkloadParser {
    public SchedulingWorkload parseScheduling(Reader source) throws IOException {
        Properties properties = load(source);
        requireType(properties, "scheduling-v1");
        long seed = longValue(properties, "seed");
        int count = positiveInt(properties, "process.count");
        int arrivalMax = nonNegativeInt(properties, "arrival.max");
        int burstMin = positiveInt(properties, "burst.min");
        int burstMax = positiveInt(properties, "burst.max");
        int priorityMin = nonNegativeInt(properties, "priority.min");
        int priorityMax = nonNegativeInt(properties, "priority.max");
        requireRange("burst", burstMin, burstMax);
        requireRange("priority", priorityMin, priorityMax);

        SplittableRandom random = new SplittableRandom(seed);
        List<ProcessSpec> processes = new ArrayList<>(count);
        for (int processId = 1; processId <= count; processId++) {
            processes.add(new ProcessSpec(
                    processId,
                    random.nextInt(arrivalMax + 1),
                    random.nextInt(burstMin, burstMax + 1),
                    random.nextInt(priorityMin, priorityMax + 1)));
        }
        return new SchedulingWorkload(
                seed, count, arrivalMax, burstMin, burstMax,
                priorityMin, priorityMax, processes);
    }

    public PagingWorkload parsePaging(Reader source) throws IOException {
        Properties properties = load(source);
        requireType(properties, "paging-locality-v1");
        long seed = longValue(properties, "seed");
        int length = positiveInt(properties, "trace.length");
        int pageCount = positiveInt(properties, "page.count");
        int frameCount = positiveInt(properties, "frame.count");
        int localityPercent = nonNegativeInt(properties, "locality.percent");
        int phaseLength = positiveInt(properties, "phase.length");
        if (frameCount > pageCount) {
            throw new IllegalArgumentException("frame.count must not exceed page.count");
        }
        if (localityPercent > 100) {
            throw new IllegalArgumentException("locality.percent must be in 0..100");
        }

        SplittableRandom random = new SplittableRandom(seed);
        List<Integer> references = new ArrayList<>(length);
        int currentPage = random.nextInt(pageCount);
        for (int index = 0; index < length; index++) {
            if (index > 0 && index % phaseLength == 0) {
                currentPage = random.nextInt(pageCount);
            } else if (index > 0 && random.nextInt(100) < localityPercent) {
                currentPage = Math.floorMod(
                        currentPage + random.nextInt(-1, 2), pageCount);
            } else if (index > 0) {
                currentPage = random.nextInt(pageCount);
            }
            references.add(currentPage);
        }
        return new PagingWorkload(
                seed, length, pageCount, frameCount,
                localityPercent, phaseLength, references);
    }

    public SchedulingWorkload loadDefaultScheduling() throws IOException {
        try (InputStream stream = resource("workloads/scheduling.properties");
             Reader reader = new java.io.InputStreamReader(
                     stream, java.nio.charset.StandardCharsets.UTF_8)) {
            return parseScheduling(reader);
        }
    }

    public PagingWorkload loadDefaultPaging() throws IOException {
        try (InputStream stream = resource("workloads/paging.properties");
             Reader reader = new java.io.InputStreamReader(
                     stream, java.nio.charset.StandardCharsets.UTF_8)) {
            return parsePaging(reader);
        }
    }

    private static Properties load(Reader source) throws IOException {
        Objects.requireNonNull(source, "source");
        Properties properties = new Properties();
        properties.load(source);
        return properties;
    }

    private static InputStream resource(String name) {
        InputStream stream = BenchmarkWorkloadParser.class
                .getClassLoader().getResourceAsStream(name);
        if (stream == null) {
            throw new IllegalStateException("Missing classpath resource: " + name);
        }
        return stream;
    }

    private static void requireType(Properties properties, String expected) {
        String actual = required(properties, "type");
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                    "Expected workload type " + expected + " but found " + actual);
        }
    }

    private static int positiveInt(Properties properties, String key) {
        int value = intValue(properties, key);
        if (value <= 0) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return value;
    }

    private static int nonNegativeInt(Properties properties, String key) {
        int value = intValue(properties, key);
        if (value < 0) {
            throw new IllegalArgumentException(key + " must be non-negative");
        }
        return value;
    }

    private static int intValue(Properties properties, String key) {
        try {
            return Integer.parseInt(required(properties, key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be an integer", exception);
        }
    }

    private static long longValue(Properties properties, String key) {
        try {
            return Long.parseLong(required(properties, key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be an integer", exception);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing property: " + key);
        }
        return value.trim();
    }

    private static void requireRange(String name, int minimum, int maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException(name + " minimum exceeds maximum");
        }
    }

    public record SchedulingWorkload(
            long seed,
            int processCount,
            int arrivalMax,
            int burstMin,
            int burstMax,
            int priorityMin,
            int priorityMax,
            List<ProcessSpec> processes
    ) {
        public SchedulingWorkload {
            processes = List.copyOf(processes);
            if (processes.size() != processCount) {
                throw new IllegalArgumentException("Process count does not match workload");
            }
        }
    }

    public record PagingWorkload(
            long seed,
            int traceLength,
            int pageCount,
            int frameCount,
            int localityPercent,
            int phaseLength,
            List<Integer> references
    ) {
        public PagingWorkload {
            references = List.copyOf(references);
            if (references.size() != traceLength) {
                throw new IllegalArgumentException("Trace length does not match workload");
            }
        }
    }
}
