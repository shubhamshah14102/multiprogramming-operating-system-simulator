package os.memory;

import java.util.Arrays;

/**
 * Explicit deterministic swap/backing store. Copies prevent callers from
 * bypassing page-in/page-out operations.
 */
public final class InMemoryBackingStore implements BackingStore {
    private final int[][] pages;
    private final int pageSize;

    public InMemoryBackingStore(int pageCount, int pageSize) {
        if (pageCount <= 0 || pageSize <= 0) {
            throw new IllegalArgumentException("Page count and size must be positive");
        }
        this.pageSize = pageSize;
        pages = new int[pageCount][pageSize];
    }

    @Override
    public int pageCount() {
        return pages.length;
    }

    @Override
    public int pageSize() {
        return pageSize;
    }

    @Override
    public int[] readPage(int pageNumber) {
        requirePage(pageNumber);
        return Arrays.copyOf(pages[pageNumber], pageSize);
    }

    @Override
    public void writePage(int pageNumber, int[] words) {
        requirePage(pageNumber);
        if (words.length != pageSize) {
            throw new IllegalArgumentException("Page has wrong size");
        }
        pages[pageNumber] = Arrays.copyOf(words, pageSize);
    }

    @Override
    public void clear() {
        for (int[] page : pages) {
            Arrays.fill(page, 0);
        }
    }

    private void requirePage(int pageNumber) {
        if (pageNumber < 0 || pageNumber >= pages.length) {
            throw new IndexOutOfBoundsException(
                    "Backing-store page out of range: " + pageNumber);
        }
    }
}
