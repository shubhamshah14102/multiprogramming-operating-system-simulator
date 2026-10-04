package os.memory;

import java.util.Comparator;
import java.util.List;

/**
 * Offline teaching policy. The constructor receives the complete future page
 * reference trace; it evicts the page whose next use is farthest away.
 */
public final class OptimalReplacementPolicy implements PageReplacementPolicy {
    private final List<Integer> pageTrace;

    public OptimalReplacementPolicy(List<Integer> pageTrace) {
        if (pageTrace == null || pageTrace.stream().anyMatch(page -> page < 0)) {
            throw new IllegalArgumentException("Page trace must contain non-negative pages");
        }
        this.pageTrace = List.copyOf(pageTrace);
    }

    @Override
    public FrameTable.Frame selectVictim(
            List<FrameTable.Frame> occupiedFrames, int requestedPage, long accessIndex) {
        if (occupiedFrames.isEmpty()) {
            throw new IllegalArgumentException("No occupied frames");
        }
        return occupiedFrames.stream()
                .max(Comparator.comparingInt(
                                (FrameTable.Frame frame) ->
                                        nextUse(frame.pageNumber(), accessIndex))
                        .thenComparingInt(frame -> -frame.frameNumber()))
                .orElseThrow();
    }

    private int nextUse(int pageNumber, long accessIndex) {
        int start = (int) Math.min(pageTrace.size(), accessIndex + 1);
        for (int i = start; i < pageTrace.size(); i++) {
            if (pageTrace.get(i) == pageNumber) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public String name() {
        return "OPTIMAL";
    }

    public List<Integer> pageTrace() {
        return pageTrace;
    }
}
