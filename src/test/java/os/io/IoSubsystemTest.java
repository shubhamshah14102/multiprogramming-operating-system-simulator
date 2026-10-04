package os.io;

import org.junit.jupiter.api.Test;
import os.interrupt.InterruptType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IoSubsystemTest {
    @Test
    void boundedBufferAppliesDeterministicBackPressure() {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(2);

        assertTrue(buffer.offer(10));
        assertTrue(buffer.offer(20));
        assertFalse(buffer.offer(30));
        assertEquals(10, buffer.poll().orElseThrow());
        assertTrue(buffer.offer(30));
        assertEquals(java.util.List.of(20, 30), buffer.snapshot());
    }

    @Test
    void blockingDeviceCompletesWithAnIoInterrupt() {
        BlockingIoDevice device = new BlockingIoDevice("printer", 1);
        IoRequest request = new IoRequest(4, "printer", 99, 2);

        assertTrue(device.submit(request));
        assertTrue(device.tick().isEmpty());
        IoCompletion completion = device.tick().orElseThrow();

        assertEquals(request, completion.request());
        assertEquals(InterruptType.IO, completion.interrupt().type());
        assertEquals(4, completion.interrupt().code());
    }

    @Test
    void spoolerRetainsOutputUntilTheDeviceCanAcceptIt() {
        Spooler spooler = new Spooler(2);
        BlockingIoDevice device = new BlockingIoDevice("printer", 1);
        IoRequest first = new IoRequest(1, "printer", 10, 3);
        IoRequest second = new IoRequest(2, "printer", 20, 1);

        assertTrue(device.submit(first));
        assertTrue(spooler.submit(second));
        assertTrue(spooler.drainOneTo(device));
        assertEquals(0, spooler.size());
        assertEquals(java.util.List.of(second), device.queuedRequests());
    }
}
