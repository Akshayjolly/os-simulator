package ie.griffith.ossim.scheduling;

import java.util.*;

/**
 * Round Robin: each process runs for at most `quantum` time units, then goes to the back of the queue.
 * Processes that arrive during a time slice are queued before the preempted process.
 */
public class RoundRobinScheduler implements Scheduler {
    private final int quantum;

    public RoundRobinScheduler(int quantum) {
        if (quantum <= 0) throw new IllegalArgumentException("quantum must be > 0");
        this.quantum = quantum;
    }

    public String name() { return "Round Robin (q=" + quantum + ")"; }

    public ScheduleResult schedule(List<Process> processes) {
        List<Process> incoming = new ArrayList<>(Scheduler.byArrival(processes));
        Map<String, Integer> remaining = new HashMap<>();
        processes.forEach(p -> remaining.put(p.pid(), p.burst()));
        Deque<Process> ready = new ArrayDeque<>();
        List<GanttSlice> gantt = new ArrayList<>();
        int time = 0;

        while (!incoming.isEmpty() || !ready.isEmpty()) {
            time = admit(incoming, ready, time);
            if (ready.isEmpty()) {
                int next = incoming.get(0).arrival();
                gantt.add(new GanttSlice(GanttSlice.IDLE, time, next));
                time = next;
                continue;
            }
            Process p = ready.poll();
            int run = Math.min(quantum, remaining.get(p.pid()));
            gantt.add(new GanttSlice(p.pid(), time, time + run));
            time += run;
            remaining.merge(p.pid(), -run, Integer::sum);
            admit(incoming, ready, time);            // new arrivals go in first
            if (remaining.get(p.pid()) > 0) ready.add(p);
        }
        return new ScheduleResult(gantt, processes);
    }

    /** Moves every process that has arrived by `time` into the ready queue. */
    private int admit(List<Process> incoming, Deque<Process> ready, int time) {
        while (!incoming.isEmpty() && incoming.get(0).arrival() <= time) ready.add(incoming.remove(0));
        return time;
    }
}
