package os.machine;

/**
 * Helpers for encoding the simulator's 32-bit instructions.
 *
 * <p>Bits 31..24 contain the opcode, 23..20 register A, 19..16 register B,
 * and 15..0 the immediate value or address.</p>
 */
public final class Instruction {
    private static final int REGISTER_COUNT = 8;

    private Instruction() {
    }

    public static int noOperands(Opcode opcode) {
        return encode(opcode, 0, 0, 0);
    }

    public static int registerImmediate(Opcode opcode, int register, int immediate) {
        if (immediate < Short.MIN_VALUE || immediate > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Immediate must fit in 16 signed bits");
        }
        return encode(opcode, register, 0, immediate);
    }

    public static int registerAddress(Opcode opcode, int register, int address) {
        requireAddress(address);
        return encode(opcode, register, 0, address);
    }

    public static int registers(Opcode opcode, int registerA, int registerB) {
        return encode(opcode, registerA, registerB, 0);
    }

    public static int address(Opcode opcode, int address) {
        requireAddress(address);
        return encode(opcode, 0, 0, address);
    }

    public static int registerAndAddress(Opcode opcode, int register, int address) {
        requireAddress(address);
        return encode(opcode, register, 0, address);
    }

    private static int encode(Opcode opcode, int registerA, int registerB, int lowBits) {
        requireRegister(registerA);
        requireRegister(registerB);
        return opcode.code() << 24
                | registerA << 20
                | registerB << 16
                | lowBits & 0xFFFF;
    }

    private static void requireRegister(int register) {
        if (register < 0 || register >= REGISTER_COUNT) {
            throw new IllegalArgumentException("Register index must be between 0 and 7");
        }
    }

    private static void requireAddress(int address) {
        if (address < 0 || address > 0xFFFF) {
            throw new IllegalArgumentException("Address must fit in 16 unsigned bits");
        }
    }
}
