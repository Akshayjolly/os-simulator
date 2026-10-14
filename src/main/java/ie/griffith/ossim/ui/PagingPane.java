package ie.griffith.ossim.ui;

import ie.griffith.ossim.memory.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Arrays;
import java.util.List;

/** Runs FIFO and LRU on the same reference string and shows each step side by side. */
public class PagingPane extends BorderPane {
    private final TextField refs = new TextField("7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1");
    private final Spinner<Integer> frames = new Spinner<>(1, 8, 3);
    private final TextArea output = new TextArea();

    public PagingPane() {
        output.setEditable(false);
        output.setStyle("-fx-font-family: monospace");
        refs.setPrefWidth(400);
        Button run = new Button("Simulate");
        run.setOnAction(e -> run());
        HBox controls = new HBox(10, new Label("Reference string:"), refs, new Label("Frames:"), frames, run);
        controls.setPadding(new Insets(10));
        setTop(controls);
        setCenter(output);
        run();
    }

    private void run() {
        try {
            int[] r = Arrays.stream(refs.getText().trim().split("[\\s,]+")).mapToInt(Integer::parseInt).toArray();
            StringBuilder sb = new StringBuilder();
            for (PageReplacement algo : List.of(new FifoReplacement(), new LruReplacement())) {
                List<PageStep> steps = algo.simulate(r, frames.getValue());
                sb.append("=== ").append(algo.name()).append(" - ").append(PageReplacement.faults(steps))
                        .append(" page faults out of ").append(r.length).append(" references ===\n");
                for (PageStep s : steps) {
                    sb.append(String.format("ref %2d -> ", s.page()));
                    for (int f : s.frames()) sb.append(f < 0 ? " . " : String.format("%2d ", f));
                    sb.append(s.fault() ? "  FAULT" : "  hit").append('\n');
                }
                sb.append('\n');
            }
            output.setText(sb.toString());
        } catch (NumberFormatException e) {
            output.setText("Reference string must be numbers separated by spaces");
        }
    }
}
