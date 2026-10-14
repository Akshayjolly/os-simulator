package ie.griffith.ossim.scheduling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Shortest Job First (non-preemptive): when the CPU is free, pick the arrived process with the smallest burst. */
public class SjfScheduler implements Scheduler {
    public String name() { return "SJF"; }

    public ScheduleResult schedule(List<Process> processes) {
        List<Process> waiting = new ArrayList<>(Scheduler.byArrival(processes));
        List<GanttSlice> gantt = new ArrayList<>();
        int time = 0;
        while (!waiting.isEmpty()) {
            final int now = time;
            Process next = waiting.stream()
                    .filter(p -> p.arrival() <= now)
                    .min(Comparator.comparingInt(Process::burst).thenComparingInt(Process::arrival))
                    .orElse(null);
            if (next == null) {                      // nothing has arrived yet
                int nextArrival = waiting.get(0).arrival();
                gantt.add(new GanttSlice(GanttSlice.IDLE, time, nextArrival));
                time = nextArrival;
                continue;
            }
            gantt.add(new GanttSlice(next.pid(), time, time + next.burst()));
            time += next.burst();
            waiting.remove(next);
        }
        return new ScheduleResult(gantt, processes);
    }
}
