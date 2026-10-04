package os.memory;

/** Mutable hardware-style page-table bits, owned by PageTable. */
public final class PageTableEntry {
    private final int pageNumber;
    private int frameNumber = -1;
    private boolean present;
    private boolean dirty;
    private boolean referenced;

    PageTableEntry(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    void map(int frameNumber) {
        this.frameNumber = frameNumber;
        present = true;
        dirty = false;
        referenced = false;
    }

    void unmap() {
        frameNumber = -1;
        present = false;
        dirty = false;
        referenced = false;
    }

    void access(boolean write) {
        referenced = true;
        dirty |= write;
    }

    public int pageNumber() {
        return pageNumber;
    }

    public int frameNumber() {
        return frameNumber;
    }

    public boolean present() {
        return present;
    }

    public boolean dirty() {
        return dirty;
    }

    public boolean referenced() {
        return referenced;
    }
}
