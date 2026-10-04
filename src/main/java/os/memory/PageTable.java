package os.memory;

import java.util.Arrays;
import java.util.List;

public final class PageTable {
    private final PageTableEntry[] entries;

    public PageTable(int pageCount) {
        if (pageCount <= 0) {
            throw new IllegalArgumentException("Page count must be positive");
        }
        entries = new PageTableEntry[pageCount];
        Arrays.setAll(entries, PageTableEntry::new);
    }

    public PageTableEntry entry(int pageNumber) {
        if (pageNumber < 0 || pageNumber >= entries.length) {
            throw new IndexOutOfBoundsException(
                    "Virtual page out of range: " + pageNumber);
        }
        return entries[pageNumber];
    }

    public int pageCount() {
        return entries.length;
    }

    public void map(int pageNumber, int frameNumber) {
        if (frameNumber < 0) {
            throw new IllegalArgumentException("Frame number cannot be negative");
        }
        entry(pageNumber).map(frameNumber);
    }

    public void unmap(int pageNumber) {
        entry(pageNumber).unmap();
    }

    public List<PageTableEntry> entries() {
        return List.copyOf(Arrays.asList(entries));
    }

    void clear() {
        for (PageTableEntry entry : entries) {
            entry.unmap();
        }
    }
}
