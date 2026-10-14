package ie.griffith.ossim.scheduling;

/**
 * Per-process results.
 * turnaround = completion - arrival, waiting = turnaround - burst,
 * response = first time on CPU - arrival.
 */
public record ProcessMetrics(String pid, int arrival, int burst, int completion, int turnaround, int waiting, int response) { }
