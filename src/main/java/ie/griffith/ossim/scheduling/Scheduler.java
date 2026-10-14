package ie.griffith.ossim.scheduling;

import java.util.Comparator;
import java.util.List;

/** Common interface for every CPU scheduling algorithm. */
public interface Scheduler {
    String name();

    ScheduleResult schedule(List<Process> processes);

    /** Processes sorted by arrival time (ties broken by pid) - every scheduler starts from this. */
    static List<Process> byArrival(List<Process> processes) {
        return processes.stream()
                .sorted(Comparator.comparingInt(Process::arrival).thenComparing(Process::pid))
                .toList();
    }
}
