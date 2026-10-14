package ie.griffith.ossim.memory;

/** FIFO: evict the page that was loaded the longest time ago. */
public class FifoReplacement extends PageReplacement {
    private int[] loadedAt;

    public String name() { return "FIFO"; }

    protected void reset(int frameCount) {
        loadedAt = new int[frameCount];
    }

    protected void onAccess(int frameIndex, int time, boolean loaded) {
        if (loaded) loadedAt[frameIndex] = time;   // hits do NOT change FIFO order
    }

    protected int chooseVictim() {
        int victim = 0;
        for (int i = 1; i < loadedAt.length; i++) if (loadedAt[i] < loadedAt[victim]) victim = i;
        return victim;
    }
}
