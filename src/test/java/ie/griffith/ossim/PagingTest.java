package ie.griffith.ossim;

import ie.griffith.ossim.memory.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagingTest {
    // Silberschatz textbook reference string
    private final int[] refs = {7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1};

    @Test
    void fifoTextbookResult() {
        assertEquals(15, PageReplacement.faults(new FifoReplacement().simulate(refs, 3)));
    }

    @Test
    void lruTextbookResult() {
        assertEquals(12, PageReplacement.faults(new LruReplacement().simulate(refs, 3)));
    }

    @Test
    void beladysAnomalyWithFifo() {
        int[] r = {1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5};
        int three = PageReplacement.faults(new FifoReplacement().simulate(r, 3));
        int four = PageReplacement.faults(new FifoReplacement().simulate(r, 4));
        assertEquals(9, three);
        assertEquals(10, four); // more frames, MORE faults
    }

    @Test
    void stepsShowFrameContents() {
        List<PageStep> steps = new LruReplacement().simulate(new int[]{1, 2, 1}, 2);
        assertEquals(List.of(1, -1), steps.get(0).frames());
        assertTrue(steps.get(1).fault());
        assertFalse(steps.get(2).fault());
    }
}
