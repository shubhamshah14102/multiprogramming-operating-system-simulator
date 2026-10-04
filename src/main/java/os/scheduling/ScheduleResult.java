package os.scheduling;

import java.util.List;
import java.util.Map;

public record ScheduleResult(
        String policy,
        List<ExecutionSlice> timeline,
        Map<Integer, ProcessMetrics> metrics,
        int contextSwitches,
        int finishTime
) {
    public ScheduleResult {
        timeline = List.copyOf(timeline);
        metrics = Map.copyOf(metrics);
    }

    public List<Integer> dispatchOrder() {
        return timeline.stream()
                .filter(slice -> !slice.idle())
                .map(ExecutionSlice::processId)
                .toList();
    }
}
