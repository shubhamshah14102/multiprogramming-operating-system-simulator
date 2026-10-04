package os.memory;

import java.util.List;

public interface PageReplacementPolicy {
    FrameTable.Frame selectVictim(
            List<FrameTable.Frame> occupiedFrames, int requestedPage, long accessIndex);

    default void recordAccess(int pageNumber, long accessIndex) {
    }

    String name();
}
