package os.machine;

/**
 * The decoded fields of one 32-bit instruction word.
 */
public record DecodedInstruction(
        int word,
        Opcode opcode,
        int registerA,
        int registerB,
        int immediate,
        int address
) {
}
