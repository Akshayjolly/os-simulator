package ie.griffith.ossim.scheduling;

import java.util.*;

/**
 * Multilevel Feedback Queue with 3 levels:
 *   level 0: Round Robin, quantum 2   (new processes start here)
 *   level 1: Round Robin, quantum 4
 *   level 2: FCFS (runs to completion)
 * A process that uses its whole quantum is demoted one level. Higher levels always run first,
 * and a new arrival in a higher level preempts a running lower-level process.
 */
public class MlfqScheduler implements Scheduler {
    private final int[] quanta = {2, 4, Integer.MAX_VALUE};

    public String name() { return "MLFQ (2 / 4 / FCFS)"; }

    public ScheduleResult schedule(List<Process> processes) {
        List<Process> incoming = new ArrayList<>(Scheduler.byArrival(processes));
        Map<String, Integer> remaining = new HashMap<>();
        processes.forEach(p -> remaining.put(p.pid(), p.burst()));
        List<Deque<Process>> queues = List.of(new ArrayDeque<>(), new ArrayDeque<>(), new ArrayDeque<>());
        List<GanttSlice> gantt = new ArrayList<>();
        int time = 0;

        while (!incoming.isEmpty() || queues.stream().anyMatch(q -> !q.isEmpty())) {
            admit(incoming, queues.get(0), time);
            int level = highestNonEmpty(queues);
            if (level < 0) {
                int next = incoming.get(0).arrival();
                gantt.add(new GanttSlice(GanttSlice.IDLE, time, next));
                time = next;
                continue;
            }
            Process p = queues.get(level).poll();
            int used = 0;
            // run one time unit at a time so a new top-level arrival can preempt us
            while (used < quanta[level] && remaining.get(p.pid()) > 0) {
                gantt.add(new GanttSlice(p.pid(), time, time + 1));
                time++;
                used++;
                remaining.merge(p.pid(), -1, Integer::sum);
                admit(incoming, queues.get(0), time);
                if (level > 0 && !queues.get(0).isEmpty()) break; // preempted by higher level
            }
            if (remaining.get(p.pid()) == 0) continue;           // finished
            if (used == quanta[level]) queues.get(Math.min(level + 1, 2)).add(p); // used full quantum -> demote
            else queues.get(level).addFirst(p);                    // preempted -> keep its level, run next
        }
        return new ScheduleResult(gantt, processes);
    }

    private void admit(List<Process> incoming, Deque<Process> top, int time) {
        while (!incoming.isEmpty() && incoming.get(0).arrival() <= time) top.add(incoming.remove(0));
    }

    private int highestNonEmpty(List<Deque<Process>> queues) {
        for (int i = 0; i < queues.size(); i++) if (!queues.get(i).isEmpty()) return i;
        return -1;
    }
}
