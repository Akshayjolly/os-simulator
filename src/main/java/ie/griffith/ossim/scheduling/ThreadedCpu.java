package ie.griffith.ossim.scheduling;

import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Replays a Gantt chart with real Java threads. Each process is a thread that blocks on its own
 * Semaphore; a "dispatcher" (the calling thread) gives the CPU to one process at a time by
 * releasing its semaphore and waits until that time slice is done. This shows how an OS
 * dispatcher context-switches between processes, using only java.util.concurrent primitives.
 */
public class ThreadedCpu {
    /** Called for every time unit a process "executes" (e.g. to update the UI or a log). */
    public interface Listener {
        void onTick(String pid, int time, String threadName);
    }

    private final Map<String, Semaphore> turn = new HashMap<>();
    private final Semaphore sliceDone = new Semaphore(0);
    private volatile GanttSlice current;
    private final long tickMillis;

    public ThreadedCpu(long tickMillis) {
        this.tickMillis = tickMillis;
    }

    public void run(ScheduleResult result, Listener listener) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        Set<String> pids = new LinkedHashSet<>();
        result.gantt().stream().map(GanttSlice::pid).filter(p -> !p.equals(GanttSlice.IDLE)).forEach(pids::add);

        for (String pid : pids) {
            Semaphore s = new Semaphore(0);
            turn.put(pid, s);
            int slicesForPid = (int) result.gantt().stream().filter(g -> g.pid().equals(pid)).count();
            Thread t = new Thread(() -> {
                try {
                    for (int i = 0; i < slicesForPid; i++) {
                        s.acquire();                      // wait to be dispatched
                        GanttSlice slice = current;
                        for (int time = slice.start(); time < slice.end(); time++) {
                            listener.onTick(pid, time, Thread.currentThread().getName());
                            if (tickMillis > 0) Thread.sleep(tickMillis);
                        }
                        sliceDone.release();              // give the CPU back
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "thread-" + pid);
            threads.add(t);
            t.start();
        }

        // dispatcher loop
        for (GanttSlice slice : result.gantt()) {
            if (slice.pid().equals(GanttSlice.IDLE)) {
                for (int time = slice.start(); time < slice.end(); time++) listener.onTick(GanttSlice.IDLE, time, "dispatcher");
                continue;
            }
            current = slice;
            turn.get(slice.pid()).release();
            sliceDone.acquire();
        }
        for (Thread t : threads) t.join();
    }
}
