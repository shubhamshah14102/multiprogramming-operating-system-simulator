package os.io;

import os.interrupt.Interrupt;

public record IoCompletion(IoRequest request, Interrupt interrupt, int completionTick) {
}
