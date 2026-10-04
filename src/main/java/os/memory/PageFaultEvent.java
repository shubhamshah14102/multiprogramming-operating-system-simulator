package os.memory;

/** A completed demand-page operation, including any replacement write-back. */
public record PageFaultEvent(
        int requestedPage,
        int assignedFrame,
        Integer evictedPage,
        boolean dirtyWriteBack,
        long accessIndex
) {
}
