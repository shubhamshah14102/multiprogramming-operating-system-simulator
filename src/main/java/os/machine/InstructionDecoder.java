package os.machine;

/**
 * Decodes fetched words independently from instruction execution.
 */
public final class InstructionDecoder {
    public DecodedInstruction decode(int word) {
        Opcode opcode = Opcode.fromCode(word >>> 24 & 0xFF);
        int registerA = word >>> 20 & 0x0F;
        int registerB = word >>> 16 & 0x0F;
        int address = word & 0xFFFF;
        int immediate = (short) address;
        return new DecodedInstruction(
                word, opcode, registerA, registerB, immediate, address);
    }
}
