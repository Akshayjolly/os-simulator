package ie.griffith.ossim;

import ie.griffith.ossim.deadlock.BankersAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankersTest {
    private BankersAlgorithm textbook() {
        int[] available = {3, 3, 2};
        int[][] max = {{7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}, {4, 3, 3}};
        int[][] alloc = {{0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}, {0, 0, 2}};
        return new BankersAlgorithm(available, max, alloc);
    }

    @Test
    void textbookStateIsSafe() {
        assertEquals(List.of(1, 3, 4, 0, 2), textbook().safeSequence());
    }

    @Test
    void safeRequestIsGranted() {
        BankersAlgorithm b = textbook();
        assertTrue(b.request(1, new int[]{1, 0, 2}));
        assertArrayEquals(new int[]{2, 3, 0}, b.available());
    }

    @Test
    void unsafeRequestIsDeniedAndRolledBack() {
        BankersAlgorithm b = textbook();
        assertTrue(b.request(1, new int[]{1, 0, 2}));
        assertFalse(b.request(0, new int[]{0, 2, 0})); // would leave system unsafe
        assertArrayEquals(new int[]{2, 3, 0}, b.available());
    }

    @Test
    void unsafeStateDetected() {
        BankersAlgorithm b = new BankersAlgorithm(new int[]{0, 0}, new int[][]{{2, 2}, {2, 2}}, new int[][]{{1, 1}, {1, 1}});
        assertFalse(b.isSafe());
    }

    @Test
    void requestOverMaxThrows() {
        assertThrows(IllegalArgumentException.class, () -> textbook().request(3, new int[]{5, 0, 0}));
    }
}
