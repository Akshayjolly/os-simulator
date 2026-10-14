package ie.griffith.ossim.memory;

/** LRU: evict the page that has not been used for the longest time. */
public class LruReplacement extends PageReplacement {
    private int[] lastUsed;

    public String name() { return "LRU"; }

    protected void reset(int frameCount) {
        lastUsed = new int[frameCount];
    }

    protected void onAccess(int frameIndex, int time, boolean loaded) {
        lastUsed[frameIndex] = time;               // every use (hit or fault) updates the time
    }

    protected int chooseVictim() {
        int victim = 0;
        for (int i = 1; i < lastUsed.length; i++) if (lastUsed[i] < lastUsed[victim]) victim = i;
        return victim;
    }
}
