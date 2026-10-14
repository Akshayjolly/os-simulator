package ie.griffith.ossim;

import ie.griffith.ossim.scheduling.*;
import ie.griffith.ossim.scheduling.Process;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThreadedCpuTest {
    @Test
    void threadsRunInExactlyTheScheduledOrder() throws InterruptedException {
        ScheduleResult r = new RoundRobinScheduler(2).schedule(List.of(
                new Process("P1", 0, 5), new Process("P2", 0, 3), new Process("P3", 1, 2)));
        List<String> log = Collections.synchronizedList(new ArrayList<>());
        List<String> threadNames = Collections.synchronizedList(new ArrayList<>());
        new ThreadedCpu(0).run(r, (pid, time, thread) -> { log.add(time + ":" + pid); threadNames.add(thread); });

        List<String> expected = new ArrayList<>();
        for (GanttSlice s : r.gantt()) for (int t = s.start(); t < s.end(); t++) expected.add(t + ":" + s.pid());
        assertEquals(expected, log);
        assertTrue(threadNames.contains("thread-P2"));
    }
}
