package ie.griffith.ossim.ui;

import ie.griffith.ossim.scheduling.*;
import ie.griffith.ossim.scheduling.Process;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Enter processes as "pid arrival burst" lines, choose an algorithm, see the Gantt chart and metrics.
 * "Run with threads" replays the schedule using ThreadedCpu so you can watch threads take turns.
 */
public class SchedulingPane extends BorderPane {
    private final TextArea input = new TextArea("P1 0 7\nP2 2 4\nP3 4 1\nP4 5 4");
    private final ComboBox<String> algo = new ComboBox<>();
    private final Spinner<Integer> quantum = new Spinner<>(1, 10, 2);
    private final Canvas gantt = new Canvas(940, 110);
    private final TextArea output = new TextArea();
    private final Color[] colours = {Color.STEELBLUE, Color.SEAGREEN, Color.DARKORANGE, Color.MEDIUMPURPLE,
            Color.INDIANRED, Color.GOLDENROD, Color.TEAL, Color.SLATEGRAY};

    public SchedulingPane() {
        algo.getItems().addAll("FCFS", "SJF", "Round Robin", "MLFQ");
        algo.getSelectionModel().selectFirst();
        input.setPrefRowCount(6);
        output.setEditable(false);
        output.setStyle("-fx-font-family: monospace");
        Button run = new Button("Schedule");
        Button threads = new Button("Run with threads");
        run.setOnAction(e -> schedule());
        threads.setOnAction(e -> runWithThreads());

        HBox controls = new HBox(10, new Label("Algorithm:"), algo, new Label("RR quantum:"), quantum, run, threads);
        VBox top = new VBox(8, new Label("Processes (pid arrival burst):"), input, controls);
        top.setPadding(new Insets(10));
        setTop(top);
        VBox centre = new VBox(8, gantt, output);
        centre.setPadding(new Insets(10));
        VBox.setVgrow(output, Priority.ALWAYS);
        setCenter(centre);
    }

    private Scheduler scheduler() {
        return switch (algo.getValue()) {
            case "SJF" -> new SjfScheduler();
            case "Round Robin" -> new RoundRobinScheduler(quantum.getValue());
            case "MLFQ" -> new MlfqScheduler();
            default -> new FcfsScheduler();
        };
    }

    private List<Process> parse() {
        List<Process> list = new ArrayList<>();
        for (String line : input.getText().split("\n")) {
            if (line.isBlank()) continue;
            String[] p = line.trim().split("\\s+");
            list.add(new Process(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2])));
        }
        return list;
    }

    private ScheduleResult schedule() {
        try {
            Scheduler s = scheduler();
            ScheduleResult r = s.schedule(parse());
            drawGantt(r);
            StringBuilder sb = new StringBuilder(s.name() + "\n\n");
            sb.append(String.format("%-6s %7s %5s %10s %10s %7s %8s%n", "PID", "Arrival", "Burst", "Completion", "Turnaround", "Waiting", "Response"));
            for (ProcessMetrics m : r.metrics()) {
                sb.append(String.format("%-6s %7d %5d %10d %10d %7d %8d%n", m.pid(), m.arrival(), m.burst(),
                        m.completion(), m.turnaround(), m.waiting(), m.response()));
            }
            sb.append(String.format("%nAverage waiting %.2f | turnaround %.2f | response %.2f | CPU utilisation %.1f%%%n",
                    r.averageWaiting(), r.averageTurnaround(), r.averageResponse(), r.cpuUtilisation()));
            output.setText(sb.toString());
            return r;
        } catch (RuntimeException e) {
            output.setText("Input error: " + e.getMessage() + "\nUse one process per line: pid arrival burst");
            return null;
        }
    }

    private void runWithThreads() {
        ScheduleResult r = schedule();
        if (r == null) return;
        output.appendText("\nThread log:\n");
        Thread worker = new Thread(() -> {
            try {
                new ThreadedCpu(150).run(r, (pid, time, thread) ->
                        Platform.runLater(() -> output.appendText("t=" + time + "  " + pid + " running on " + thread + "\n")));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, "dispatcher");
        worker.setDaemon(true);
        worker.start();
    }

    private void drawGantt(ScheduleResult r) {
        GraphicsContext g = gantt.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, gantt.getWidth(), gantt.getHeight());
        List<GanttSlice> slices = r.gantt();
        if (slices.isEmpty()) return;
        int end = slices.get(slices.size() - 1).end();
        double unit = (gantt.getWidth() - 20) / end;
        List<String> pids = r.metrics().stream().map(ProcessMetrics::pid).toList();
        for (GanttSlice s : slices) {
            double x = 10 + s.start() * unit, w = s.length() * unit;
            g.setFill(s.pid().equals(GanttSlice.IDLE) ? Color.LIGHTGRAY : colours[Math.max(0, pids.indexOf(s.pid())) % colours.length]);
            g.fillRect(x, 20, w, 50);
            g.setStroke(Color.BLACK);
            g.strokeRect(x, 20, w, 50);
            g.setFill(Color.WHITE);
            g.fillText(s.pid(), x + 4, 50);
            g.setFill(Color.BLACK);
            g.fillText(String.valueOf(s.start()), x, 88);
        }
        g.fillText(String.valueOf(end), 10 + end * unit - 8, 88);
    }
}
