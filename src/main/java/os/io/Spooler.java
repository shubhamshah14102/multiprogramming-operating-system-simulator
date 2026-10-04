package os.io;

import java.util.List;
import java.util.Objects;

/**
 * Stages output requests in a bounded spool so producers do not own the slow
 * device. drainOne preserves a job when the device queue is full.
 */
public final class Spooler {
    private final BoundedBuffer<IoRequest> spool;

    public Spooler(int capacity) {
        spool = new BoundedBuffer<>(capacity);
    }

    public boolean submit(IoRequest request) {
        return spool.offer(Objects.requireNonNull(request, "request"));
    }

    public boolean drainOneTo(BlockingIoDevice device) {
        Objects.requireNonNull(device, "device");
        if (spool.isEmpty()) {
            return false;
        }
        IoRequest next = spool.snapshot().getFirst();
        if (!device.submit(next)) {
            return false;
        }
        spool.poll();
        return true;
    }

    public List<IoRequest> pending() {
        return spool.snapshot();
    }

    public int size() {
        return spool.size();
    }
}
