package os.machine;

import java.util.Arrays;

/**
 * Word-addressable physical memory.
 */
public final class MachineMemory implements Memory {
    private final int[] words;

    public MachineMemory(int wordCount) {
        if (wordCount <= 0 || wordCount > 0x10000) {
            throw new IllegalArgumentException("Memory size must be between 1 and 65536 words");
        }
        words = new int[wordCount];
    }

    public int read(int address) {
        requireAddress(address);
        return words[address];
    }

    public void write(int address, int value) {
        requireAddress(address);
        words[address] = value;
    }

    public int size() {
        return words.length;
    }

    public void clear() {
        Arrays.fill(words, 0);
    }

    private void requireAddress(int address) {
        if (address < 0 || address >= words.length) {
            throw new IndexOutOfBoundsException("Memory address out of range: " + address);
        }
    }
}
