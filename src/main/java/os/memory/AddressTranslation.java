package os.memory;

/** The arithmetic of converting a virtual word address. */
public record AddressTranslation(
        int virtualAddress,
        int pageNumber,
        int offset,
        int frameNumber,
        int physicalAddress
) {
}
