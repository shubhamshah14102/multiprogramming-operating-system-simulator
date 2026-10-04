package os.memory;

import os.machine.MachineMemory;
import os.machine.Memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Single-address-space demand-paged memory. Page tables and frame ownership are
 * intentionally exposed for observation, while all data movement goes through
 * the explicit physical memory and backing store.
 */
public final class PagedVirtualMemory implements Memory {
    private final int pageSize;
    private final MachineMemory physicalMemory;
    private final BackingStore backingStore;
    private final PageTable pageTable;
    private final FrameTable frameTable;
    private final AddressTranslator translator;
    private final PageReplacementPolicy replacementPolicy;
    private final List<PageFaultEvent> pageFaults = new ArrayList<>();
    private long accessIndex;

    public PagedVirtualMemory(
            int virtualPageCount,
            int pageSize,
            int frameCount,
            PageReplacementPolicy replacementPolicy
    ) {
        this(
                new InMemoryBackingStore(virtualPageCount, pageSize),
                new MachineMemory(Math.multiplyExact(frameCount, pageSize)),
                replacementPolicy);
    }

    public PagedVirtualMemory(
            BackingStore backingStore,
            MachineMemory physicalMemory,
            PageReplacementPolicy replacementPolicy
    ) {
        this.backingStore = Objects.requireNonNull(backingStore, "backingStore");
        this.physicalMemory = Objects.requireNonNull(physicalMemory, "physicalMemory");
        this.replacementPolicy =
                Objects.requireNonNull(replacementPolicy, "replacementPolicy");
        pageSize = backingStore.pageSize();
        if (physicalMemory.size() % pageSize != 0) {
            throw new IllegalArgumentException(
                    "Physical memory size must be a multiple of page size");
        }
        pageTable = new PageTable(backingStore.pageCount());
        frameTable = new FrameTable(physicalMemory.size() / pageSize);
        translator = new AddressTranslator(pageSize, pageTable);
    }

    @Override
    public int read(int address) {
        AddressTranslation translation = prepareAccess(address, false);
        return physicalMemory.read(translation.physicalAddress());
    }

    @Override
    public void write(int address, int value) {
        AddressTranslation translation = prepareAccess(address, true);
        physicalMemory.write(translation.physicalAddress(), value);
    }

    public AddressTranslation translate(int virtualAddress) {
        requireAddress(virtualAddress);
        ensureResident(virtualAddress / pageSize);
        return translator.translate(virtualAddress);
    }

    private AddressTranslation prepareAccess(int address, boolean write) {
        requireAddress(address);
        int page = address / pageSize;
        ensureResident(page);
        AddressTranslation translation = translator.translate(address);
        pageTable.entry(page).access(write);
        frameTable.frame(translation.frameNumber()).touch(accessIndex);
        replacementPolicy.recordAccess(page, accessIndex);
        accessIndex++;
        return translation;
    }

    private void ensureResident(int requestedPage) {
        if (pageTable.entry(requestedPage).present()) {
            return;
        }

        FrameTable.Frame frame = frameTable.freeFrame().orElseGet(() ->
                replacementPolicy.selectVictim(
                        frameTable.occupiedFrames(), requestedPage, accessIndex));
        Integer evictedPage = frame.pageNumber() < 0 ? null : frame.pageNumber();
        boolean dirtyWriteBack = false;
        if (evictedPage != null) {
            PageTableEntry victim = pageTable.entry(evictedPage);
            if (victim.dirty()) {
                backingStore.writePage(
                        evictedPage, readPhysicalFrame(frame.frameNumber()));
                dirtyWriteBack = true;
            }
            victim.unmap();
        }

        writePhysicalFrame(frame.frameNumber(), backingStore.readPage(requestedPage));
        frame.occupy(requestedPage, accessIndex);
        pageTable.entry(requestedPage).map(frame.frameNumber());
        pageFaults.add(new PageFaultEvent(
                requestedPage,
                frame.frameNumber(),
                evictedPage,
                dirtyWriteBack,
                accessIndex));
    }

    private int[] readPhysicalFrame(int frameNumber) {
        int[] page = new int[pageSize];
        int base = frameNumber * pageSize;
        for (int offset = 0; offset < pageSize; offset++) {
            page[offset] = physicalMemory.read(base + offset);
        }
        return page;
    }

    private void writePhysicalFrame(int frameNumber, int[] page) {
        int base = frameNumber * pageSize;
        for (int offset = 0; offset < pageSize; offset++) {
            physicalMemory.write(base + offset, page[offset]);
        }
    }

    private void requireAddress(int address) {
        if (address < 0 || address >= size()) {
            throw new IndexOutOfBoundsException(
                    "Virtual address out of range: " + address);
        }
    }

    @Override
    public int size() {
        return pageTable.pageCount() * pageSize;
    }

    @Override
    public void clear() {
        physicalMemory.clear();
        backingStore.clear();
        pageTable.clear();
        frameTable.clear();
        pageFaults.clear();
        accessIndex = 0;
    }

    public int pageSize() {
        return pageSize;
    }

    public long pageFaultCount() {
        return pageFaults.size();
    }

    public List<PageFaultEvent> pageFaultHistory() {
        return List.copyOf(pageFaults);
    }

    public PageTable pageTable() {
        return pageTable;
    }

    public FrameTable frameTable() {
        return frameTable;
    }

    public BackingStore backingStore() {
        return backingStore;
    }

    public MachineMemory physicalMemory() {
        return physicalMemory;
    }

    public PageReplacementPolicy replacementPolicy() {
        return replacementPolicy;
    }
}
