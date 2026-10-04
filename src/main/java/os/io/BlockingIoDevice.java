package os.io;

import os.interrupt.Interrupt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One active operation plus a bounded waiting queue. submit returning false
 * means the caller must remain ready and retry; accepted callers may block
 * until the completion interrupt is produced.
 */
public final class BlockingIoDevice {
    private final String name;
    private final BoundedBuffer<IoRequest> waiting;
    private final List<IoCompletion> completions = new ArrayList<>();
    private IoRequest active;
    private int remainingTicks;
    private int clock;

    public BlockingIoDevice(String name, int queueCapacity) {
        this.name = Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Device name cannot be blank");
        }
        waiting = new BoundedBuffer<>(queueCapacity);
    }

    public boolean submit(IoRequest request) {
        Objects.requireNonNull(request, "request");
        if (!name.equals(request.deviceName())) {
            throw new IllegalArgumentException(
                    "Request targets " + request.deviceName() + ", not " + name);
        }
        if (active == null) {
            begin(request);
            return true;
        }
        return waiting.offer(request);
    }

    public Optional<IoCompletion> tick() {
        clock++;
        if (active == null) {
            startNext();
            return Optional.empty();
        }

        remainingTicks--;
        if (remainingTicks > 0) {
            return Optional.empty();
        }

        IoRequest finished = active;
        IoCompletion completion = new IoCompletion(
                finished, Interrupt.io(finished.processId(), name), clock);
        completions.add(completion);
        active = null;
        startNext();
        return Optional.of(completion);
    }

    private void startNext() {
        waiting.poll().ifPresent(this::begin);
    }

    private void begin(IoRequest request) {
        active = request;
        remainingTicks = request.duration();
    }

    public boolean busy() {
        return active != null;
    }

    public Optional<IoRequest> activeRequest() {
        return Optional.ofNullable(active);
    }

    public List<IoRequest> queuedRequests() {
        return waiting.snapshot();
    }

    public List<IoCompletion> completions() {
        return List.copyOf(completions);
    }

    public int clock() {
        return clock;
    }

    public String name() {
        return name;
    }
}
