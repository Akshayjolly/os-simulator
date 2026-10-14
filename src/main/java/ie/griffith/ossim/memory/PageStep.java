package ie.griffith.ossim.memory;

import java.util.List;

/** State after one page reference: the frames' contents (-1 = empty frame) and whether it was a fault. */
public record PageStep(int page, List<Integer> frames, boolean fault) { }
