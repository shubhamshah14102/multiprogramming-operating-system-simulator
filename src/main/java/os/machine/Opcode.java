package os.machine;

import java.util.Arrays;

/**
 * Opcodes for the simulator's fixed-width, 32-bit instruction set.
 */
public enum Opcode {
    NOP(0x00),
    LOAD_IMMEDIATE(0x01),
    LOAD(0x02),
    STORE(0x03),
    MOVE(0x04),
    ADD(0x05),
    SUBTRACT(0x06),
    JUMP(0x07),
    JUMP_IF_ZERO(0x08),
    SVC(0x09),
    HALT(0x0A),
    SET_TIMER(0x0B);

    private final int code;

    Opcode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static Opcode fromCode(int code) {
        return Arrays.stream(values())
                .filter(opcode -> opcode.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown opcode: 0x%02X".formatted(code)));
    }
}
