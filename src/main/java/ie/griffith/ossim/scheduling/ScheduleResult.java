package ie.griffith.ossim.scheduling;

import java.util.*;

/** Gantt chart plus metrics produced by a scheduler. */
public class ScheduleResult {
    private final List<GanttSlice> gantt;
    private final List<ProcessMetrics> metrics;

    public ScheduleResult(List<GanttSlice> gantt, List<Process> processes) {
        this.gantt = mergeAdjacent(gantt);
        this.metrics = computeMetrics(this.gantt, processes);
    }

    public List<GanttSlice> gantt() { return gantt; }
    public List<ProcessMetrics> metrics() { return metrics; }

    public double averageWaiting() {
        return metrics.stream().mapToInt(ProcessMetrics::waiting).average().orElse(0);
    }

    public double averageTurnaround() {
        return metrics.stream().mapToInt(ProcessMetrics::turnaround).average().orElse(0);
    }

    public double averageResponse() {
        return metrics.stream().mapToInt(ProcessMetrics::response).average().orElse(0);
    }

    /** Percentage of time the CPU was busy. */
    public double cpuUtilisation() {
        if (gantt.isEmpty()) return 0;
        int total = gantt.get(gantt.size() - 1).end() - gantt.get(0).start();
        int busy = gantt.stream().filter(s -> !s.pid().equals(GanttSlice.IDLE)).mapToInt(GanttSlice::length).sum();
        return 100.0 * busy / total;
    }

    /** Joins neighbouring slices of the same process so the chart is tidy. */
    private static List<GanttSlice> mergeAdjacent(List<GanttSlice> in) {
        List<GanttSlice> out = new ArrayList<>();
        for (GanttSlice s : in) {
            if (s.length() == 0) continue;
            if (!out.isEmpty()) {
                GanttSlice last = out.get(out.size() - 1);
                if (last.pid().equals(s.pid()) && last.end() == s.start()) {
                    out.set(out.size() - 1, new GanttSlice(s.pid(), last.start(), s.end()));
                    continue;
                }
            }
            out.add(s);
        }
        return out;
    }

    private static List<ProcessMetrics> computeMetrics(List<GanttSlice> gantt, List<Process> processes) {
        List<ProcessMetrics> list = new ArrayList<>();
        for (Process p : processes) {
            int first = Integer.MAX_VALUE, last = 0;
            for (GanttSlice s : gantt) {
                if (s.pid().equals(p.pid())) {
                    first = Math.min(first, s.start());
                    last = Math.max(last, s.end());
                }
            }
            int turnaround = last - p.arrival();
            list.add(new ProcessMetrics(p.pid(), p.arrival(), p.burst(), last, turnaround,
                    turnaround - p.burst(), first - p.arrival()));
        }
        return list;
    }
}
