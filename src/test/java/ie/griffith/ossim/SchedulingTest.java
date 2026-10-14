package ie.griffith.ossim;

import ie.griffith.ossim.scheduling.*;
import ie.griffith.ossim.scheduling.Process;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingTest {
    // Classic textbook example
    private final List<Process> procs = List.of(
            new Process("P1", 0, 7), new Process("P2", 2, 4),
            new Process("P3", 4, 1), new Process("P4", 5, 4));

    private static String order(ScheduleResult r) {
        return r.gantt().stream().map(GanttSlice::pid).collect(Collectors.joining(","));
    }

    @Test
    void fcfsRunsInArrivalOrder() {
        ScheduleResult r = new FcfsScheduler().schedule(procs);
        assertEquals("P1,P2,P3,P4", order(r));
        // waiting: P1=0, P2=5, P3=7, P4=7 -> 19/4
        assertEquals(4.75, r.averageWaiting(), 1e-9);
    }

    @Test
    void sjfPicksShortestArrivedJob() {
        ScheduleResult r = new SjfScheduler().schedule(procs);
        assertEquals("P1,P3,P2,P4", order(r));
        assertEquals(4.0, r.averageWaiting(), 1e-9); // 0 + 6 + 3 + 7
    }

    @Test
    void roundRobinTimeSlices() {
        ScheduleResult r = new RoundRobinScheduler(2).schedule(procs);
        assertEquals("P1,P2,P1,P3,P2,P4,P1,P4,P1", order(r));
        assertEquals(16, r.gantt().get(r.gantt().size() - 1).end());
        r.metrics().forEach(m -> assertTrue(m.waiting() >= 0));
    }

    @Test
    void idleTimeIsShownWhenNoProcessHasArrived() {
        ScheduleResult r = new FcfsScheduler().schedule(List.of(new Process("A", 3, 2)));
        assertEquals("IDLE,A", order(r));
        assertEquals(40.0, r.cpuUtilisation(), 1e-9);
    }

    @Test
    void mlfqDemotesLongJobsAndFavoursShortOnes() {
        List<Process> ps = List.of(new Process("Long", 0, 10), new Process("Short", 1, 2));
        ScheduleResult r = new MlfqScheduler().schedule(ps);
        // Long uses its level-0 quantum (2), then Short (level 0) runs before Long continues at level 1
        assertEquals("Long,Short,Long", order(r));
        ProcessMetrics shortJob = r.metrics().get(1);
        assertEquals(4, shortJob.completion());
        assertEquals(12, r.metrics().get(0).completion());
    }

    @Test
    void mlfqNewArrivalPreemptsLowerLevel() {
        List<Process> ps = List.of(new Process("A", 0, 8), new Process("B", 3, 1));
        ScheduleResult r = new MlfqScheduler().schedule(ps);
        // A: 0-2 at level 0, then level 1 from 2; B arrives at 3 and preempts
        assertEquals("A,B,A", order(r));
        assertEquals(4, r.metrics().get(1).completion());
    }

    @Test
    void everySchedulerFinishesAllWork() {
        for (Scheduler s : List.of(new FcfsScheduler(), new SjfScheduler(), new RoundRobinScheduler(3), new MlfqScheduler())) {
            ScheduleResult r = s.schedule(procs);
            for (Process p : procs) {
                int ran = r.gantt().stream().filter(g -> g.pid().equals(p.pid())).mapToInt(GanttSlice::length).sum();
                assertEquals(p.burst(), ran, s.name() + " " + p.pid());
            }
        }
    }

    @Test
    void invalidProcessRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Process("X", 0, 0));
    }
}
