package os.memory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagedVirtualMemoryTest {
    @Test
    void translatesPageAndOffsetThroughThePageTable() {
        PageTable table = new PageTable(4);
        table.map(2, 1);

        AddressTranslation translation = new AddressTranslator(4, table).translate(10);

        assertEquals(2, translation.pageNumber());
        assertEquals(2, translation.offset());
        assertEquals(1, translation.frameNumber());
        assertEquals(6, translation.physicalAddress());
    }

    @Test
    void demandPagesWritesBackDirtyFifoVictimAndRestoresItsData() {
        PagedVirtualMemory memory =
                new PagedVirtualMemory(3, 2, 2, new FifoReplacementPolicy());

        memory.write(0, 77);
        memory.read(2);
        memory.read(4);

        PageFaultEvent replacement = memory.pageFaultHistory().get(2);
        assertEquals(0, replacement.evictedPage());
        assertTrue(replacement.dirtyWriteBack());
        assertFalse(memory.pageTable().entry(0).present());

        assertEquals(77, memory.read(0));
        assertEquals(4, memory.pageFaultCount());
    }

    @Test
    void lruAndOfflineOptimalChooseDifferentObservableVictims() {
        PagedVirtualMemory lru =
                new PagedVirtualMemory(3, 1, 2, new LruReplacementPolicy());
        lru.read(0);
        lru.read(1);
        lru.read(0);
        lru.read(2);
        assertEquals(1, lru.pageFaultHistory().getLast().evictedPage());

        PagedVirtualMemory optimal = new PagedVirtualMemory(
                3, 1, 2,
                new OptimalReplacementPolicy(List.of(0, 1, 2, 0, 1)));
        optimal.read(0);
        optimal.read(1);
        optimal.read(2);
        assertEquals(1, optimal.pageFaultHistory().getLast().evictedPage());
    }
}
