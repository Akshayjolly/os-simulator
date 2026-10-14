package ie.griffith.ossim.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for page replacement algorithms. Subclasses only decide WHICH frame to evict;
 * this class handles the frames, hits and faults.
 */
public abstract class PageReplacement {
    public abstract String name();

    /** Called on every reference so the algorithm can update its bookkeeping. */
    protected abstract void onAccess(int frameIndex, int time, boolean loaded);

    /** Choose a frame to evict when all frames are full. */
    protected abstract int chooseVictim();

    protected abstract void reset(int frameCount);

    public List<PageStep> simulate(int[] references, int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("need at least one frame");
        reset(frameCount);
        List<Integer> frames = new ArrayList<>(Collections.nCopies(frameCount, null));
        List<PageStep> steps = new ArrayList<>();
        for (int t = 0; t < references.length; t++) {
            int page = references[t];
            int index = frames.indexOf(page);
            boolean fault = index < 0;
            if (fault) {
                index = frames.indexOf(null);           // free frame?
                if (index < 0) index = chooseVictim();  // no -> evict
                frames.set(index, page);
            }
            onAccess(index, t, fault);
            steps.add(new PageStep(page, List.copyOf(withNulls(frames)), fault));
        }
        return steps;
    }

    public static int faults(List<PageStep> steps) {
        return (int) steps.stream().filter(PageStep::fault).count();
    }

    // List.copyOf rejects nulls, so empty frames are stored as -1 in the snapshot.
    private static List<Integer> withNulls(List<Integer> frames) {
        List<Integer> out = new ArrayList<>();
        for (Integer f : frames) out.add(f == null ? -1 : f);
        return out;
    }
}
