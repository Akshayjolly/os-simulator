package ie.griffith.ossim.deadlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Banker's algorithm for deadlock avoidance.
 * n processes, m resource types.
 *   available[m]        free instances of each resource
 *   max[n][m]           most each process may ever need
 *   allocation[n][m]    what each process holds now
 *   need = max - allocation
 */
public class BankersAlgorithm {
    private final int[] available;
    private final int[][] max;
    private final int[][] allocation;

    public BankersAlgorithm(int[] available, int[][] max, int[][] allocation) {
        if (max.length != allocation.length) throw new IllegalArgumentException("max and allocation sizes differ");
        this.available = available.clone();
        this.max = deepCopy(max);
        this.allocation = deepCopy(allocation);
        for (int i = 0; i < max.length; i++) {
            for (int j = 0; j < available.length; j++) {
                if (allocation[i][j] > max[i][j]) throw new IllegalArgumentException("P" + i + " holds more than its max");
            }
        }
    }

    public int[][] need() {
        int[][] need = new int[max.length][available.length];
        for (int i = 0; i < max.length; i++)
            for (int j = 0; j < available.length; j++) need[i][j] = max[i][j] - allocation[i][j];
        return need;
    }

    public int[] available() {
        return available.clone();
    }

    /** Safety algorithm. Returns a safe sequence of process indices, or an empty list if the state is unsafe. */
    public List<Integer> safeSequence() {
        int[] work = available.clone();
        boolean[] finished = new boolean[max.length];
        int[][] need = need();
        List<Integer> sequence = new ArrayList<>();
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < max.length; i++) {
                if (!finished[i] && lessOrEqual(need[i], work)) {
                    // pretend P_i finishes and gives back everything it holds
                    for (int j = 0; j < work.length; j++) work[j] += allocation[i][j];
                    finished[i] = true;
                    sequence.add(i);
                    progress = true;
                }
            }
        }
        return sequence.size() == max.length ? sequence : List.of();
    }

    public boolean isSafe() {
        return !safeSequence().isEmpty() || max.length == 0;
    }

    /**
     * Resource-request algorithm. Grants the request only if it keeps the system safe.
     * @return true if granted (state updated), false if the process must wait
     */
    public boolean request(int process, int[] request) {
        int[] need = need()[process];
        if (!lessOrEqual(request, need)) throw new IllegalArgumentException("P" + process + " asked for more than its max");
        if (!lessOrEqual(request, available)) return false;

        // try it out
        for (int j = 0; j < request.length; j++) {
            available[j] -= request[j];
            allocation[process][j] += request[j];
        }
        if (isSafe()) return true;
        // roll back - granting it would be unsafe
        for (int j = 0; j < request.length; j++) {
            available[j] += request[j];
            allocation[process][j] -= request[j];
        }
        return false;
    }

    private static boolean lessOrEqual(int[] a, int[] b) {
        for (int j = 0; j < a.length; j++) if (a[j] > b[j]) return false;
        return true;
    }

    private static int[][] deepCopy(int[][] m) {
        return Arrays.stream(m).map(int[]::clone).toArray(int[][]::new);
    }
}
