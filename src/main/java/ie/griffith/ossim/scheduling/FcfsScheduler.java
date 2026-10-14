package ie.griffith.ossim.scheduling;

import java.util.ArrayList;
import java.util.List;

/** First Come First Served: run processes in arrival order, each to completion. */
public class FcfsScheduler implements Scheduler {
    public String name() { return "FCFS"; }

    public ScheduleResult schedule(List<Process> processes) {
        List<GanttSlice> gantt = new ArrayList<>();
        int time = 0;
        for (Process p : Scheduler.byArrival(processes)) {
            if (time < p.arrival()) {               // CPU idle until the process arrives
                gantt.add(new GanttSlice(GanttSlice.IDLE, time, p.arrival()));
                time = p.arrival();
            }
            gantt.add(new GanttSlice(p.pid(), time, time + p.burst()));
            time += p.burst();
        }
        return new ScheduleResult(gantt, processes);
    }
}
