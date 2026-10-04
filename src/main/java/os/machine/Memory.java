package os.machine;

/**
 * Word-addressable memory boundary used by the CPU. A paged address translator
 * can implement this contract in a later phase.
 */
public interface Memory {
    int read(int address);

    void write(int address, int value);

    int size();

    void clear();
}
