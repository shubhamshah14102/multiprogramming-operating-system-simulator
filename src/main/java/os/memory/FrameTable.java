package os.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FrameTable {
    private final Frame[] frames;

    public FrameTable(int frameCount) {
        if (frameCount <= 0) {
            throw new IllegalArgumentException("Frame count must be positive");
        }
        frames = new Frame[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = new Frame(i);
        }
    }

    public Optional<Frame> freeFrame() {
        for (Frame frame : frames) {
            if (frame.pageNumber < 0) {
                return Optional.of(frame);
            }
        }
        return Optional.empty();
    }

    public Frame frame(int frameNumber) {
        if (frameNumber < 0 || frameNumber >= frames.length) {
            throw new IndexOutOfBoundsException("Frame out of range: " + frameNumber);
        }
        return frames[frameNumber];
    }

    public List<Frame> occupiedFrames() {
        List<Frame> occupied = new ArrayList<>();
        for (Frame frame : frames) {
            if (frame.pageNumber >= 0) {
                occupied.add(frame);
            }
        }
        return List.copyOf(occupied);
    }

    public int frameCount() {
        return frames.length;
    }

    void clear() {
        for (Frame frame : frames) {
            frame.release();
        }
    }

    public static final class Frame {
        private final int frameNumber;
        private int pageNumber = -1;
        private long loadedAt = -1;
        private long lastAccessedAt = -1;

        private Frame(int frameNumber) {
            this.frameNumber = frameNumber;
        }

        void occupy(int pageNumber, long time) {
            this.pageNumber = pageNumber;
            loadedAt = time;
            lastAccessedAt = time;
        }

        void touch(long time) {
            lastAccessedAt = time;
        }

        void release() {
            pageNumber = -1;
            loadedAt = -1;
            lastAccessedAt = -1;
        }

        public int frameNumber() {
            return frameNumber;
        }

        public int pageNumber() {
            return pageNumber;
        }

        public long loadedAt() {
            return loadedAt;
        }

        public long lastAccessedAt() {
            return lastAccessedAt;
        }
    }
}
