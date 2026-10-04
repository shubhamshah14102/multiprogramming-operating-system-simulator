package os.loader;

import os.process.ProcessControlBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Converts parsed jobs to PCBs without reordering them.
 */
public final class JobLoader {
    public List<ProcessControlBlock> load(List<JobDefinition> jobs) {
        Objects.requireNonNull(jobs, "jobs");
        Set<Integer> processIds = new HashSet<>();
        List<ProcessControlBlock> processes = new ArrayList<>(jobs.size());

        for (JobDefinition job : jobs) {
            Objects.requireNonNull(job, "job");
            if (!processIds.add(job.processId())) {
                throw new IllegalArgumentException(
                        "Duplicate process id: " + job.processId());
            }
            processes.add(new ProcessControlBlock(
                    job.processId(), job.priority(), job.program()));
        }
        return List.copyOf(processes);
    }
}
