package os.memory;

/**
 * Translation-level page fault. PagedVirtualMemory catches it and performs
 * demand paging; callers using AddressTranslator directly can inspect it.
 */
public final class PageFaultException extends RuntimeException {
    private final int pageNumber;
    private final int virtualAddress;

    public PageFaultException(int pageNumber, int virtualAddress) {
        super("Page " + pageNumber + " is not resident for address " + virtualAddress);
        this.pageNumber = pageNumber;
        this.virtualAddress = virtualAddress;
    }

    public int pageNumber() {
        return pageNumber;
    }

    public int virtualAddress() {
        return virtualAddress;
    }
}
