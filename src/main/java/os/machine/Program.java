package os.machine;

import java.util.Arrays;

/**
 * Immutable sequence of encoded instruction words.
 */
public final class Program {
    private final int[] words;

    public Program(int... words) {
        if (words.length == 0) {
            throw new IllegalArgumentException("A program must contain at least one instruction");
        }
        this.words = Arrays.copyOf(words, words.length);
    }

    public int wordAt(int address) {
        if (address < 0 || address >= words.length) {
            throw new IndexOutOfBoundsException("Program address out of range: " + address);
        }
        return words[address];
    }

    public int size() {
        return words.length;
    }

    public int[] words() {
        return Arrays.copyOf(words, words.length);
    }
}
