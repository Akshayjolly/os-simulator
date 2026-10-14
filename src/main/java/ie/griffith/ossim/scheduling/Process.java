package ie.griffith.ossim.scheduling;

/**
 * A process to be scheduled.
 * @param pid     name shown in the Gantt chart, e.g. "P1"
 * @param arrival time the process arrives in the ready queue
 * @param burst   CPU time it needs
 */
public record Process(String pid, int arrival, int burst) {
    public Process {
        if (arrival < 0) throw new IllegalArgumentException("arrival must be >= 0");
        if (burst <= 0) throw new IllegalArgumentException("burst must be > 0");
    }
}
