package os.memory;

public interface BackingStore {
    int pageCount();

    int pageSize();

    int[] readPage(int pageNumber);

    void writePage(int pageNumber, int[] words);

    void clear();
}
