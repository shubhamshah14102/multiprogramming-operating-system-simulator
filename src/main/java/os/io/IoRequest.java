package os.io;

import java.util.Objects;

public record IoRequest(int processId, String deviceName, int data, int duration) {
    public IoRequest {
        if (processId < 0 || duration <= 0) {
            throw new IllegalArgumentException(
                    "Process id must be non-negative and duration positive");
        }
        deviceName = Objects.requireNonNull(deviceName, "deviceName");
        if (deviceName.isBlank()) {
            throw new IllegalArgumentException("Device name cannot be blank");
        }
    }
}
