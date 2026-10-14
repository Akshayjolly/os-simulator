package ie.griffith.ossim.scheduling;

/** One bar of the Gantt chart: process pid ran from start (inclusive) to end (exclusive). pid "IDLE" means no process. */
public record GanttSlice(String pid, int start, int end) {
    public static final String IDLE = "IDLE";

    public int length() {
        return end - start;
    }
}
