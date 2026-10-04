package os.memory;

import java.util.Objects;

public final class AddressTranslator {
    private final int pageSize;
    private final PageTable pageTable;

    public AddressTranslator(int pageSize, PageTable pageTable) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        this.pageSize = pageSize;
        this.pageTable = Objects.requireNonNull(pageTable, "pageTable");
    }

    public AddressTranslation translate(int virtualAddress) {
        if (virtualAddress < 0
                || virtualAddress >= pageSize * pageTable.pageCount()) {
            throw new IndexOutOfBoundsException(
                    "Virtual address out of range: " + virtualAddress);
        }
        int page = virtualAddress / pageSize;
        int offset = virtualAddress % pageSize;
        PageTableEntry entry = pageTable.entry(page);
        if (!entry.present()) {
            throw new PageFaultException(page, virtualAddress);
        }
        int physical = entry.frameNumber() * pageSize + offset;
        return new AddressTranslation(
                virtualAddress, page, offset, entry.frameNumber(), physical);
    }
}
