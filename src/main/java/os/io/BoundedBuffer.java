package os.io;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Non-blocking bounded producer/consumer buffer. A false offer makes
 * back-pressure explicit to the simulated producer.
 */
public final class BoundedBuffer<T> {
    private final int capacity;
    private final ArrayDeque<T> elements = new ArrayDeque<>();

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
    }

    public boolean offer(T element) {
        Objects.requireNonNull(element, "element");
        if (elements.size() == capacity) {
            return false;
        }
        elements.addLast(element);
        return true;
    }

    public Optional<T> poll() {
        return Optional.ofNullable(elements.pollFirst());
    }

    public List<T> snapshot() {
        return List.copyOf(elements);
    }

    public int size() {
        return elements.size();
    }

    public int capacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public boolean isFull() {
        return elements.size() == capacity;
    }
}
