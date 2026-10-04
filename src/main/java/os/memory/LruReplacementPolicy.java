package os.memory;

import java.util.Comparator;
import java.util.List;

public final class LruReplacementPolicy implements PageReplacementPolicy {
    @Override
    public FrameTable.Frame selectVictim(
            List<FrameTable.Frame> occupiedFrames, int requestedPage, long accessIndex) {
        return occupiedFrames.stream()
                .min(Comparator.comparingLong(FrameTable.Frame::lastAccessedAt)
                        .thenComparingInt(FrameTable.Frame::frameNumber))
                .orElseThrow(() -> new IllegalArgumentException("No occupied frames"));
    }

    @Override
    public String name() {
        return "LRU";
    }
}
