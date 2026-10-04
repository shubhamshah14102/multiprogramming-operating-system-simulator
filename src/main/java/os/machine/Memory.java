package os.machine;

/** Word-addressable memory used by the CPU. */
public interface Memory {
    int read(int address);

    void write(int address, int value);

    int size();

    void clear();
}
